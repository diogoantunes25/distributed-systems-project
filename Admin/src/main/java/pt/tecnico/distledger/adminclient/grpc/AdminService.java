package pt.tecnico.distledger.adminclient.grpc;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import pt.tecnico.distledger.adminclient.exceptions.ServerLookupFailedException;
import pt.tecnico.distledger.adminclient.exceptions.ServerUnavailableException;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger;
import pt.tecnico.distledger.namingserver.grpc.NamingServiceClient;
import pt.tecnico.distledger.namingserver.NamingServer;

public class AdminService {
    private NamingServiceClient namingServiceClient = new NamingServiceClient();
    private final static int TIMEOUT = 100; // milliseconds

    // Caches ManagedChannel for qualifier
    private final Map<String, ManagedChannel> serverCache = new HashMap<>();

    private boolean cacheHasServerEntry(String server) {
        return serverCache.containsKey(server);
    }

    private void cacheUpdate(String server, ManagedChannel channel) {
        serverCache.put(server, channel);
    }

    public void cacheRefresh(String qual) throws ServerLookupFailedException {
        List<String> servers = this.namingServiceClient.lookup(NamingServer.SERVICE_NAME, qual);
        if(servers.isEmpty()) {
            throw new ServerLookupFailedException(qual);
        }

        if (servers.size() > 1){
            // We assume there is only one secondary server active at every moment
            System.out.println("WARNING: More than one secondary server found");
        }

        cacheUpdate(qual, ManagedChannelBuilder.forTarget(servers.get(0)).usePlaintext().build());
    }

    public ManagedChannel getServerChannel(String server) {
        return serverCache.get(server);
    }

    public void activate(String server) throws ServerUnavailableException, ServerLookupFailedException {
        if (cacheHasServerEntry(server)) cacheRefresh(server);

        try {
            tryActivate(server);
        } catch (ServerUnavailableException e) {
            cacheRefresh(server);
            tryActivate(server);
        }
    }

    private void tryActivate(String server) throws ServerUnavailableException {
        ManagedChannel channel = getServerChannel(server);

        try{
            AdminServiceGrpc.AdminServiceBlockingStub stub = AdminServiceGrpc.newBlockingStub(channel);
            AdminDistLedger.ActivateRequest request = 
                AdminDistLedger.ActivateRequest.newBuilder().build();
            AdminDistLedger.ActivateResponse response = stub.withDeadlineAfter(TIMEOUT, TimeUnit.MILLISECONDS).activate(request);

            System.out.println("OK");
            System.out.println(response);
        } catch (StatusRuntimeException e) {
            channel.shutdown();

            if (e.getStatus() == Status.UNAVAILABLE) {
                throw new ServerUnavailableException(e);
            }

            System.err.println(e.getMessage());
        }
    }

    public void deactivate(String server) throws ServerUnavailableException, ServerLookupFailedException {
        if (cacheHasServerEntry(server)) cacheRefresh(server);

        try {
            tryDeactivate(server);
        } catch (ServerUnavailableException e) {
            cacheRefresh(server);
            tryDeactivate(server);
        }
    }
    private void tryDeactivate(String server) throws ServerUnavailableException {
        ManagedChannel channel = getServerChannel(server);

        try{
            AdminServiceGrpc.AdminServiceBlockingStub stub = AdminServiceGrpc.newBlockingStub(channel);
            AdminDistLedger.DeactivateRequest request =
                AdminDistLedger.DeactivateRequest.newBuilder().build();
            AdminDistLedger.DeactivateResponse response = stub.withDeadlineAfter(TIMEOUT, TimeUnit.MILLISECONDS).deactivate(request);

            System.out.println("OK");
            System.out.println(response);
        } catch (StatusRuntimeException e) {
            channel.shutdown();
            if (e.getStatus() == Status.UNAVAILABLE) {
                throw new ServerUnavailableException(e);
            }
            System.err.println(e.getMessage());
        }
    }

    public void getLedgerState(String server) throws ServerLookupFailedException, ServerUnavailableException {
        if (cacheHasServerEntry(server)) cacheRefresh(server);

        try {
            tryGetLedgerState(server);
        } catch (ServerUnavailableException e) {
            cacheRefresh(server);
            tryGetLedgerState(server);
        }
    }

    private void tryGetLedgerState(String server) throws ServerUnavailableException {
        ManagedChannel channel = getServerChannel(server);

        try{
            AdminServiceGrpc.AdminServiceBlockingStub stub = AdminServiceGrpc.newBlockingStub(channel);
            AdminDistLedger.getLedgerStateRequest request =
                AdminDistLedger.getLedgerStateRequest.newBuilder().build();
            AdminDistLedger.getLedgerStateResponse response = stub.withDeadlineAfter(TIMEOUT, TimeUnit.MILLISECONDS).getLedgerState(request);

            System.out.println("OK");
            System.out.println(response);
        } catch (StatusRuntimeException e) {
            channel.shutdown();
            if (e.getStatus() == Status.UNAVAILABLE) {
                throw new ServerUnavailableException(e);
            }
            System.err.println(e.getMessage());
        }
    }

    public void delete() {
        namingServiceClient.delete();
    }
}
