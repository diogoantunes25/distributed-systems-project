package pt.tecnico.distledger.userclient.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.tecnico.distledger.userclient.exceptions.ServerLookupFailedException;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.NamingServerDistLedger;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.NamingServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger;

public class UserService {
    private final ManagedChannel nameServerChannel;
    private final NamingServiceGrpc.NamingServiceBlockingStub nameServerStub;
    private final static String SERVICE_NAME = "UserService";

    public UserService(String nameServer) {
        this.nameServerChannel = ManagedChannelBuilder.forTarget(nameServer).usePlaintext().build();
        this.nameServerStub = NamingServiceGrpc.newBlockingStub(nameServerChannel);
    }

    public ManagedChannel getServerChannel(String server) throws ServerLookupFailedException {
        NamingServerDistLedger.LookupRequest request = NamingServerDistLedger.LookupRequest.newBuilder()
                .setServicename(SERVICE_NAME)
                .setQualifier(server)
                .build();

        try {
            NamingServerDistLedger.LookupResponse response = nameServerStub.lookup(request);
            if (response.getServicesCount() == 0) {
                throw new ServerLookupFailedException(server);
            }
            System.out.printf("Server for %s with %s found at %s\n", SERVICE_NAME, server, response.getServices(0));

            return ManagedChannelBuilder.forTarget(response.getServices(0)).usePlaintext().build();
        } catch (StatusRuntimeException e) {
            throw new ServerLookupFailedException(server, e);
        }
    }

    public void createAccount(String server, String username){

        ManagedChannel channel;
        try {
            channel = getServerChannel(server);
        } catch (ServerLookupFailedException e) {
            e.printStackTrace();
            return;
        }

        try{
            UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel);
            UserDistLedger.CreateAccountRequest request = 
                UserDistLedger.CreateAccountRequest.newBuilder().setUserId(username).build();
            UserDistLedger.CreateAccountResponse response = stub.createAccount(request);

            System.out.println("OK");
            System.out.println(response);
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getDescription());
            System.err.println(e.getMessage());
            System.out.println("");
        } finally {
            channel.shutdown();
        }
    }

    public void deleteAccount(String server, String username){

        ManagedChannel channel;
        try {
            channel = getServerChannel(server);
        } catch (ServerLookupFailedException e) {
            e.printStackTrace();
            return;
        }

        try{
            UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel);
            UserDistLedger.DeleteAccountRequest request =
                UserDistLedger.DeleteAccountRequest.newBuilder().setUserId(username).build();
            UserDistLedger.DeleteAccountResponse response = stub.deleteAccount(request);

            System.out.println("OK");
            System.out.println(response);
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getDescription());
            System.err.println(e.getMessage());
            System.out.println("");
        } finally {
            channel.shutdown();
        }
    }
    
    public void balance(String server, String username){

        ManagedChannel channel;
        try {
            channel = getServerChannel(server);
        } catch (ServerLookupFailedException e) {
            e.printStackTrace();
            return;
        }

        try{
            UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel);
            UserDistLedger.BalanceRequest request =
                UserDistLedger.BalanceRequest.newBuilder().setUserId(username).build();
            UserDistLedger.BalanceResponse response = stub.balance(request);

            System.out.println("OK");
            System.out.println(response);
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getDescription());
            System.err.println(e.getMessage());
            System.out.println("");
        } finally {
            channel.shutdown();
        }
    }

    public void transferTo(String server, String username, String dest, Integer amount){

        ManagedChannel channel;
        try {
            channel = getServerChannel(server);
        } catch (ServerLookupFailedException e) {
            e.printStackTrace();
            return;
        }

        try{
            UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel);
            UserDistLedger.TransferToRequest request =
                UserDistLedger.TransferToRequest.newBuilder()
                .setAccountFrom(username).setAccountTo(dest).setAmount(amount).build();
            UserDistLedger.TransferToResponse response = stub.transferTo(request);
            
            System.out.println("OK");
            System.out.println(response);
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getDescription());
            System.err.println(e.getMessage());
            System.out.println("");
        } finally {
            channel.shutdown();
        }
    }

    public void delete() {
        nameServerChannel.shutdown();
    }

}
