package pt.tecnico.distledger.server.grpc;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.LedgerState;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.exceptions.CannotPropagateStateException;
import pt.tecnico.distledger.server.exceptions.NoSecundaryServersException;
import pt.tecnico.distledger.server.visitor.MessageConverterVisitor;

public class CrossServerClient {
    private final String SERVICE_NAME = "DistLedger";

    private static final String PRIMARY_QUAL = "A";
    private static final String SECONDARY_QUAL = "B";

    private static final Integer TIMEOUT = 500;

    private ServerState state;
    private final String qual;
    private String cachedServer;
    private Integer cachedServerStateSize;

    public CrossServerClient(ServerState state, String qual) {
        this.cachedServer = null;
        this.cachedServerStateSize = 0;
        this.state = state;
        this.qual = qual;
    } 

    public boolean canWrite() {
        return qual.equals(PRIMARY_QUAL);
    }

    private void cacheUpdate(String server, Integer size){
        this.cachedServer = server;
        this.cachedServerStateSize = size;
    }
    
    private boolean isEmptyCache() {
        return cachedServer == null;
    }

    private void cacheRefresh() throws NoSecundaryServersException {
        List<String> secondaryServers = NamingServiceClient.lookup(SERVICE_NAME, SECONDARY_QUAL);
        if (secondaryServers.isEmpty()) {
            throw new NoSecundaryServersException();
        }

        if (secondaryServers.size() > 1){
            // We assume there is only one secondary server active at every moment
            System.out.println("WARNING: More than one secondary server found");
        }

        cacheUpdate(secondaryServers.get(0), 0);
    }

    private void tryPropagateState() 
            throws NoSecundaryServersException, CannotPropagateStateException {
        ManagedChannel channel = ManagedChannelBuilder.forTarget(cachedServer).usePlaintext().build();
        DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub stub = DistLedgerCrossServerServiceGrpc.newBlockingStub(channel);

        MessageConverterVisitor visitor = new MessageConverterVisitor();
        List<Operation> ledgerState = state.getLedgerState();
        
        LedgerState.Builder ledgerStateBuilder = LedgerState.newBuilder();
        ledgerState.subList(cachedServerStateSize, ledgerState.size())
                .forEach(o -> ledgerStateBuilder.addLedger(o.accept(visitor)));
        
        PropagateStateRequest request = PropagateStateRequest.newBuilder()
                .setState(ledgerStateBuilder.build())
                .build();

        try {
            stub.withDeadlineAfter(TIMEOUT, TimeUnit.MILLISECONDS).propagateState(request);
        } catch (StatusRuntimeException e) {
            throw new CannotPropagateStateException(cachedServer, e);
        } finally {
            channel.shutdown();
        }

    }

    public void propagateState() 
            throws NoSecundaryServersException, CannotPropagateStateException {
        if(isEmptyCache()) cacheRefresh();

        try {
            tryPropagateState();
            cacheUpdate(cachedServer, state.getLedgerState().size());
        } catch (CannotPropagateStateException e) {
            cacheRefresh();
            tryPropagateState();
            cacheUpdate(cachedServer, state.getLedgerState().size());
        }
    }
}
