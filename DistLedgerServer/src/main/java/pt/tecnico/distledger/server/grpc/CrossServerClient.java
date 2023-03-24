package pt.tecnico.distledger.server.grpc;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.LedgerState;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;
import pt.tecnico.distledger.namingserver.grpc.NamingServiceClient;
import pt.tecnico.distledger.namingserver.NamingServer;

import pt.tecnico.distledger.server.exceptions.CannotPropagateStateException;
import pt.tecnico.distledger.server.exceptions.NoSecundaryServersException;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.visitor.MessageConverterVisitor;

public class CrossServerClient {

    private static final Integer TIMEOUT = 5000; // miliseconds

    private ServerState state;
    private final String qual;

    private String cachedServer;
    private Integer cachedServerStateSize;
    private ManagedChannel cachedChannel;
    
    private NamingServiceClient namingServiceClient = new NamingServiceClient();

    public CrossServerClient(ServerState state, String qual) {
        this.cachedServer = null;
        this.cachedChannel = null;
        this.cachedServerStateSize = 0;
        this.state = state;
        this.qual = qual;
    }

    public boolean canWrite() {
        return qual.equals(NamingServer.PRIMARY_QUAL);
    }

    private void cacheUpdate(String server, Integer size, ManagedChannel channel){
        this.cachedServer = server;
        this.cachedChannel = channel;
        this.cachedServerStateSize = size;
    }
    
    private boolean isEmptyCache() {
        return cachedServer == null;
    }

    private void cacheRefresh() throws NoSecundaryServersException {
        List<String> secondaryServers = namingServiceClient.lookup(NamingServer.SERVICE_NAME, NamingServer.SECONDARY_QUAL);
        if (secondaryServers.isEmpty()) {
            throw new NoSecundaryServersException();
        }

        if (secondaryServers.size() > 1){
            // We assume there is only one secondary server active at every moment
            System.out.println("WARNING: More than one secondary server found");
        }

        cacheUpdate(secondaryServers.get(0), 0, ManagedChannelBuilder.forTarget(secondaryServers.get(0)).usePlaintext().build());
    }

    private void tryPropagateState(Operation op) 
            throws NoSecundaryServersException, CannotPropagateStateException {
        DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub stub = DistLedgerCrossServerServiceGrpc.newBlockingStub(cachedChannel);

        MessageConverterVisitor visitor = new MessageConverterVisitor();
        List<Operation> ledgerState = state.getLedgerState();
        ledgerState.add(op);
        
        LedgerState.Builder ledgerStateBuilder = LedgerState.newBuilder();
        ledgerState.subList(cachedServerStateSize, ledgerState.size())
                .forEach(o -> ledgerStateBuilder.addLedger(o.accept(visitor)));
        
        PropagateStateRequest request = PropagateStateRequest.newBuilder()
                .setState(ledgerStateBuilder.build())
                .build();

        try {
            stub.withDeadlineAfter(TIMEOUT, TimeUnit.MILLISECONDS).propagateState(request);
        } catch (StatusRuntimeException e) {
            cachedChannel.shutdown();
            throw new CannotPropagateStateException(cachedServer, e);
        }
    }

    public void propagateState(Operation op) 
            throws NoSecundaryServersException, CannotPropagateStateException {
        if (isEmptyCache()) cacheRefresh();

        try {
            tryPropagateState(op);
            cacheUpdate(cachedServer, state.getLedgerState().size(), cachedChannel); // Just update size stored
        } catch (CannotPropagateStateException e) {
            cacheRefresh();
            tryPropagateState(op);
            cacheUpdate(cachedServer, state.getLedgerState().size(), cachedChannel);
        }
    }
}
