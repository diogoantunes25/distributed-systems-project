package pt.tecnico.distledger.server.grpc;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import pt.tecnico.distledger.server.domain.operation.UpdateOp;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.LedgerState;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;
import pt.tecnico.distledger.namingserver.grpc.NamingServiceClient;
import pt.tecnico.distledger.namingserver.NamingServer;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.visitor.MessageConverterVisitor;
import pt.tecnico.distledger.server.exceptions.NoReplicasException;
import pt.tecnico.distledger.server.exceptions.CannotGossipException;

public class CrossServerClient {

    private static final Integer TIMEOUT = 5000; // miliseconds

    private ServerState state;

    private Map<String, ManagedChannel> cache;
    
    private NamingServiceClient namingServiceClient = new NamingServiceClient();

    // Saves number of log entries "gossiped" to each target
    private Map<String, Integer> gossiped = new ConcurrentHashMap<>();

    public CrossServerClient(ServerState state) {
        this.state = state;
        this.cache = new HashMap<>();
    }

    private Set<Map.Entry<String, ManagedChannel>> getCacheEntries() {
        return cache.entrySet();
    }

    private void clearCache() {
        cache.clear();
    }

    public synchronized void cacheRefresh() throws NoReplicasException {
        List<String> replicas = namingServiceClient.lookup(NamingServer.SERVICE_NAME, "").stream()
                .filter(r -> !r.equals(state.getTarget())).collect(Collectors.toList());

        System.out.printf("[CrossServerClient] refreshing cache - there are %s replicas\n", replicas.size());

        if (replicas.isEmpty()) throw new NoReplicasException();

        clearCache();
        for (String replica : replicas) {
            ManagedChannel old = cache.put(replica, ManagedChannelBuilder.forTarget(replica).usePlaintext().build());
            if (old != null) {
                try {
                    old.shutdown();
                } catch(Exception e) {
                    System.out.println("Channel shutdown failed");
                }
            }
        }
    }

    public synchronized void propagateState() throws NoReplicasException, CannotGossipException {
        ServerState.Read<List<UpdateOp>> ledgerState = state.getLedgerCopy();

        try {
            tryPropagateStateToAll(ledgerState);
        } catch (CannotGossipException e) {
            cacheRefresh();
            tryPropagateStateToAll(ledgerState);
        }
    }

    /**
     * Tries to propagate to all replicas. Blocks until a single propagation goes through. 
     * Throws exception if failed to gossip to everyone.
     */
    // TODO: add comments
    private void tryPropagateStateToAll(ServerState.Read<List<UpdateOp>> ledgerState) throws CannotGossipException {
        System.out.printf("[CrossServerClient] trying to propagate state to all\n");

        if (getCacheEntries().size() == 0) throw new CannotGossipException();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger running = new AtomicInteger(getCacheEntries().size());
        AtomicBoolean success = new AtomicBoolean(false);

        for (Map.Entry<String, ManagedChannel> entry : getCacheEntries()){
            new Thread(() -> {
                try {
                    tryPropagateStateToSingle(entry.getKey(), ledgerState);
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

    private void tryPropagateStateToSingle(String target, ServerState.Read<List<UpdateOp>> ledgerState) throws CannotGossipException {
        ManagedChannel channel = cache.get(target);
        DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub stub = DistLedgerCrossServerServiceGrpc.newBlockingStub(channel);
        try {
            LedgerState.Builder builder = LedgerState.newBuilder();

            MessageConverterVisitor visitor = new MessageConverterVisitor();
            System.out.printf("[CrossServerClient] propagating from %s (inclusive) to %s (exclusive)\n", gossiped.computeIfAbsent(target, t -> 0), ledgerState.getValue().size());
            ledgerState.getValue().subList(gossiped.computeIfAbsent(target, t -> 0), ledgerState.getValue().size())
                    .stream().forEach(op -> builder.addLedger(op.accept(visitor)));

            PropagateStateRequest request = PropagateStateRequest.newBuilder()
                    .setReplicaTS(ledgerState.getNewTs().toGrpc())
                    .setLog(builder)
                    .build();

            stub.withDeadlineAfter(TIMEOUT, TimeUnit.MILLISECONDS).propagateState(request);
            gossiped.put(target, ledgerState.getValue().size());

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
