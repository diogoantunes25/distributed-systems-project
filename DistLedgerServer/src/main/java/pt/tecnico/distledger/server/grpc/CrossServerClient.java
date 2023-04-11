package pt.tecnico.distledger.server.grpc;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import pt.tecnico.distledger.gossip.Timestamp;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.LedgerState;
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
        List<String> replicas = namingServiceClient.lookup(NamingServer.SERVICE_NAME, "");
        replicas.remove(state.getTarget());
        if (replicas.isEmpty()) {
            throw new NoReplicasException();
        }

        clearCache();
        for(String replica : replicas){
            cache.put(replica, ManagedChannelBuilder.forTarget(replica).usePlaintext().build());
        }
    }

    private void tryPropagateState(String replica, ManagedChannel channel, Timestamp prev) throws NoReplicasException, CannotGossipException {
        DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub stub = DistLedgerCrossServerServiceGrpc.newBlockingStub(channel);

        MessageConverterVisitor visitor = new MessageConverterVisitor();
        ServerState.Read<List<Operation>> ledgerState = state.getLedgerState(prev);
        
        LedgerState.Builder ledgerStateBuilder = LedgerState.newBuilder();
        ledgerState.getValue().forEach(o -> ledgerStateBuilder.addLedger(o.accept(visitor)));
        
        PropagateStateRequest request = PropagateStateRequest.newBuilder()
                .setReplicaTS(ledgerState.getNewTs().toGrpc())
                .setLog(ledgerStateBuilder.build())
                .setReplicaTS((DistLedgerCommonDefinitions.Timestamp) null) // FIXME: should be actual timestamp
                .build();

        if (isEmptyCache()) {
            cacheRefresh();

            for(Map.Entry<String, ManagedChannel> entry : getCacheEntries()){
                tryPropagateState(entry.getKey(), entry.getValue(), prev);
            }

            if (isEmptyCache()) throw new CannotGossipException();
        }
    }
}
