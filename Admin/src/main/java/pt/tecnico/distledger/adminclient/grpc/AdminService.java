package pt.tecnico.distledger.adminclient.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import io.grpc.StatusRuntimeException;
import pt.tecnico.distledger.adminclient.exceptions.ServerLookupFailedException;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.NamingServerDistLedger;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.NamingServiceGrpc;

public class AdminService {
    private final ManagedChannel nameServerChannel;
    private final NamingServiceGrpc.NamingServiceBlockingStub nameServerStub;
    private final static String SERVICE_NAME = "AdminService";

    public AdminService(String nameServer) {
        this.nameServerChannel = ManagedChannelBuilder.forTarget(nameServer).usePlaintext().build();
        this.nameServerStub = NamingServiceGrpc.newBlockingStub(nameServerChannel);
    }

    public ManagedChannel getServerChannel(String server) throws ServerLookupFailedException {
        NamingServerDistLedger.LookupRequest request = NamingServerDistLedger.LookupRequest.newBuilder()
                .setServiceName(SERVICE_NAME)
                .setQualifier(server)
                .build();

        try {
            NamingServerDistLedger.LookupResponse response = nameServerStub.lookup(request);
            if (response.getServicesCount() == 0) {
                throw new ServerLookupFailedException(server);
            }

            return ManagedChannelBuilder.forTarget(response.getServices(0)).usePlaintext().build();
        } catch (StatusRuntimeException e) {
            throw new ServerLookupFailedException(server, e);
        }
    }

    public void activate(String server){
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
            AdminDistLedger.ActivateResponse response = stub.activate(request);

            System.out.println("OK");
            System.out.println(response);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
            channel.shutdown();
        }
    }

    public void deactivate(String server){
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
            AdminDistLedger.DeactivateResponse response = stub.deactivate(request);

            System.out.println("OK");
            System.out.println(response);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
            channel.shutdown();
        }
    }
    
    public void getLedgerState(String server){
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
            AdminDistLedger.getLedgerStateResponse response = stub.getLedgerState(request);

            System.out.println("OK");
            System.out.println(response);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
            channel.shutdown();
        }
    }

    public void delete() {
        nameServerChannel.shutdown();
    }
}
