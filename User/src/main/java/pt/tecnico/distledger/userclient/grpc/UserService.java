package pt.tecnico.distledger.userclient.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pt.tecnico.distledger.userclient.exceptions.ServerLookupFailedException;
import pt.tecnico.distledger.userclient.exceptions.ServerUnavailableException;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.NamingServerDistLedger;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.NamingServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger;

import java.sql.Time;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class UserService {
    private final ManagedChannel nameServerChannel;
    private final NamingServiceGrpc.NamingServiceBlockingStub nameServerStub;
    private final static String SERVICE_NAME = "DistLedger";
    private final static int TIMEOUT = 100;

    // Caches ManagedChannel for qualifier
    private final Map<String, ManagedChannel> serverCache = new HashMap<>();

    public UserService(String nameServer) {
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

    public void createAccount(String server, String username)
            throws ServerLookupFailedException, ServerUnavailableException {
        try {
            tryCreateAccount(server, username);
        } catch (ServerUnavailableException e) {
            refreshCache(server);
            tryCreateAccount(server, username);
        }
    }

    private void tryCreateAccount(String server, String username) throws ServerUnavailableException {

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
            UserDistLedger.CreateAccountResponse response = stub.withDeadlineAfter(TIMEOUT, TimeUnit.MILLISECONDS).createAccount(request);

            System.out.println("OK");
            System.out.println(response);
        } catch (StatusRuntimeException e) {
            channel.shutdown();

            if (e.getStatus() == Status.UNAVAILABLE) {
                throw new ServerUnavailableException(e);
            }

            System.out.println(e.getStatus().getDescription());
            System.err.println(e.getMessage());
            System.out.println("");
        }
    }

    public void deleteAccount(String server, String username)
            throws ServerLookupFailedException, ServerUnavailableException {
        try {
            tryDeleteAccount(server, username);
        } catch (ServerUnavailableException e) {
            refreshCache(server);
            tryDeleteAccount(server, username);
        }
    }

    private void tryDeleteAccount(String server, String username) throws ServerUnavailableException {

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
            UserDistLedger.DeleteAccountResponse response = stub.withDeadlineAfter(TIMEOUT, TimeUnit.MILLISECONDS).deleteAccount(request);

            System.out.println("OK");
            System.out.println(response);
        } catch (StatusRuntimeException e) {
            channel.shutdown();

            if (e.getStatus() == Status.UNAVAILABLE) {
                throw new ServerUnavailableException(e);
            }

            System.out.println(e.getStatus().getDescription());
            System.err.println(e.getMessage());
            System.out.println("");
        }
    }

    public void balance(String server, String username) throws ServerUnavailableException, ServerLookupFailedException {
        try {
            tryGetBalance(server, username);
        } catch (ServerUnavailableException e) {
            refreshCache(server);
            tryGetBalance(server, username);
        }
    }

    private void tryGetBalance(String server, String username) throws ServerUnavailableException {

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
            UserDistLedger.BalanceResponse response = stub.withDeadlineAfter(TIMEOUT, TimeUnit.MILLISECONDS).balance(request);

            System.out.println("OK");
            System.out.println(response);
        } catch (StatusRuntimeException e) {
            channel.shutdown();

            if (e.getStatus() == Status.UNAVAILABLE) {
                throw new ServerUnavailableException(e);
            }

            System.out.println(e.getStatus().getDescription());
            System.err.println(e.getMessage());
            System.out.println("");
        }
    }

    public void transferTo(String server, String username, String dest, Integer amount) throws ServerLookupFailedException, ServerUnavailableException {
        try {
            tryGetBalance(server, username);
        } catch (ServerUnavailableException e) {
            refreshCache(server);
            tryGetBalance(server, username);
        }
    }

    private void tryTransferTo(String server, String username, String dest, Integer amount) throws ServerUnavailableException {

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
            UserDistLedger.TransferToResponse response = stub.withDeadlineAfter(TIMEOUT, TimeUnit.MILLISECONDS).transferTo(request);
            
            System.out.println("OK");
            System.out.println(response);
        } catch (StatusRuntimeException e) {
            channel.shutdown();

            if (e.getStatus() == Status.UNAVAILABLE) {
                throw new ServerUnavailableException(e);
            }

            System.out.println(e.getStatus().getDescription());
            System.err.println(e.getMessage());
            System.out.println("");
        }
    }

    public void delete() {
        nameServerChannel.shutdown();
    }

}
