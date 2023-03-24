package pt.tecnico.distledger.adminclient.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pt.tecnico.distledger.adminclient.exceptions.ServerLookupFailedException;
import pt.tecnico.distledger.adminclient.exceptions.ServerUnavailableException;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.NamingServerDistLedger;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.NamingServiceGrpc;

import java.sql.Time;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AdminService {
    private final ManagedChannel nameServerChannel;
    private final NamingServiceGrpc.NamingServiceBlockingStub nameServerStub;
    private final static String SERVICE_NAME = "DistLedger";
    private final static int TIMEOUT = 100; // milliseconds

    // Caches ManagedChannel for qualifier
    private final Map<String, ManagedChannel> serverCache = new HashMap<>();

    public AdminService(String nameServer) {
        this.nameServerChannel = ManagedChannelBuilder.forTarget(nameServer).usePlaintext().build();
        this.nameServerStub = NamingServiceGrpc.newBlockingStub(nameServerChannel);
    }

    public void refreshCache(String qual)
            throws ServerLookupFailedException {
        NamingServerDistLedger.LookupRequest request = NamingServerDistLedger.LookupRequest.newBuilder()
                .setServiceName(SERVICE_NAME)
                .setQualifier(qual)
                .build();
        try {
            NamingServerDistLedger.LookupResponse response = nameServerStub.lookup(request);
            if (response.getServicesCount() == 0) {
                throw new ServerLookupFailedException(qual);
            }
            System.out.printf("Server for %s with %s found at %s\n", SERVICE_NAME, qual, response.getServices(0));

            serverCache.put(qual, ManagedChannelBuilder.forTarget(response.getServices(0)).usePlaintext().build());
        } catch (StatusRuntimeException e) {
            throw new ServerLookupFailedException(qual, e);
        }
    }

    public ManagedChannel getServerChannel(String server) throws ServerLookupFailedException {
        if (!serverCache.containsKey(server)) {
            refreshCache(server);
        }

        return serverCache.get(server);
    }


    public void activate(String server) throws ServerUnavailableException, ServerLookupFailedException {
        try {
            tryActivate(server);
        } catch (ServerUnavailableException e) {
            refreshCache(server);
            tryActivate(server);
        }
    }

    private void tryActivate(String server) throws ServerUnavailableException {
        ManagedChannel channel;
        try {
            channel = getServerChannel(server);
        } catch (ServerLookupFailedException e) {
            e.printStackTrace();
            return;
        }

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
        try {
            tryActivate(server);
        } catch (ServerUnavailableException e) {
            refreshCache(server);
            tryActivate(server);
        }
    }
    private void tryDeactivate(String server) throws ServerUnavailableException {
        ManagedChannel channel;
        try {
            channel = getServerChannel(server);
        } catch (ServerLookupFailedException e) {
            e.printStackTrace();
            return;
        }

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
        try {
            tryGetLedgerState(server);
        } catch (ServerUnavailableException e) {
            refreshCache(server);
            tryGetLedgerState(server);
        }
    }

    private void tryGetLedgerState(String server) throws ServerUnavailableException {
        ManagedChannel channel;
        try {
            channel = getServerChannel(server);
        } catch (ServerLookupFailedException e) {
            e.printStackTrace();
            return;
        }

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
        nameServerChannel.shutdown();
    }
}
