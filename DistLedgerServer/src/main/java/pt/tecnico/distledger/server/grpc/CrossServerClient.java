package pt.tecnico.distledger.server.grpc;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import pt.tecnico.distledger.gossip.Timestamp;
import pt.tecnico.distledger.server.domain.exceptions.ServerUnavailableException;
import pt.tecnico.distledger.server.domain.operation.UpdateOp;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.LedgerState;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;
import pt.tecnico.distledger.namingserver.grpc.NamingServiceClient;
import pt.tecnico.distledger.namingserver.NamingServer;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.visitor.MessageConverterVisitor;
import pt.tecnico.distledger.server.exceptions.NoReplicasException;
import pt.tecnico.distledger.server.exceptions.CannotGossipException;

public class CrossServerClient {

    private static final Integer TIMEOUT = 5000; // miliseconds

    private ServerState state;
    private final String qual;

    private Map<String, ManagedChannel> cache;
    
    private NamingServiceClient namingServiceClient = new NamingServiceClient();

    public CrossServerClient(ServerState state, String qual) {
        this.state = state;
        this.qual = qual;
        this.cache = new HashMap<>();
    }

    private Set<Map.Entry<String, ManagedChannel>> getCacheEntries() {
        return cache.entrySet();
    }

    private void clearCache() {
        cache.clear();
    }

    private void removeCacheEntry(String replica) {
        cache.remove(replica);
    }

    private boolean isEmptyCache() {
        return cache.isEmpty();
    }

    private void cacheRefresh() throws NoReplicasException {
        List<String> replicas = namingServiceClient.lookup(NamingServer.SERVICE_NAME, "").stream()
                .filter(r -> !r.equals(state.getTarget())).collect(Collectors.toList());

        System.out.printf("[CrossServerClient] refreshing cache - there are %s replicas\n", replicas.size());

        if (replicas.isEmpty()) throw new NoReplicasException();

        clearCache();
        for (String replica : replicas) cache.put(replica, ManagedChannelBuilder.forTarget(replica).usePlaintext().build());
    }

    public void propagateState() throws NoReplicasException, CannotGossipException {
        if (isEmptyCache()) cacheRefresh();

        // Get state to propagate
        MessageConverterVisitor visitor = new MessageConverterVisitor();
        ServerState.Read<List<UpdateOp>> ledgerState = state.getLedgerCopy();

        LedgerState.Builder ledgerStateBuilder = LedgerState.newBuilder();
        ledgerState.getValue().forEach(o -> ledgerStateBuilder.addLedger(o.accept(visitor)));

        PropagateStateRequest request = PropagateStateRequest.newBuilder()
                .setReplicaTS(ledgerState.getNewTs().toGrpc())
                .setLog(ledgerStateBuilder.build())
                .build();

        try {
            tryPropagateStateToAll(request);
        } catch (CannotGossipException e) {
            cacheRefresh();
            tryPropagateStateToAll(request);
        }
    }

    /**
     * Tries to propagate to all replicas. Blocks until a single propagation goes thought. Throws exception if failed
     * to gossip to everyone.
     * @param request - request to use for propagation
     */
    private void tryPropagateStateToAll(PropagateStateRequest request) throws CannotGossipException {
        System.out.printf("[CrossServerClient] trying to propagate state to all\n");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger running = new AtomicInteger(getCacheEntries().size());
        AtomicBoolean success = new AtomicBoolean(false);

        for (Map.Entry<String, ManagedChannel> entry : getCacheEntries()){
            new Thread(() -> {
                try {
                    tryPropagateStateToSingle(entry.getValue(), request);
                    System.out.printf("[CrossServerClient] propagation to %s succeeded\n", entry.getKey());
                    success.set(true);
                    latch.countDown();
                } catch (CannotGossipException e) {
                    System.out.printf("[CrossServerClient] propagation to %s failed\n", entry.getKey());
                }
                finally {
                    if (running.decrementAndGet() == 0) {
                        latch.countDown();
                    }
                }
            }).start();
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (!success.get()) throw new CannotGossipException();
    }

    private void tryPropagateStateToSingle(ManagedChannel channel, PropagateStateRequest request) throws CannotGossipException {
        DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub stub = DistLedgerCrossServerServiceGrpc.newBlockingStub(channel);
        try {
            stub.withDeadlineAfter(TIMEOUT, TimeUnit.MILLISECONDS).propagateState(request);
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getDescription());
            System.err.println(e.getMessage());
            System.out.println("");

            if (e.getStatus() == Status.UNAVAILABLE) {
                channel.shutdown();
                throw new CannotGossipException();
            }
        }
    }
}
