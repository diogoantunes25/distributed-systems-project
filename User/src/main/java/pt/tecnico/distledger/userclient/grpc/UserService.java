package pt.tecnico.distledger.userclient.grpc;

import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.*;
import pt.tecnico.distledger.client.grpc.Service;
import pt.tecnico.distledger.gossip.Timestamp;

import pt.tecnico.distledger.client.exceptions.ServerLookupFailedException;
import pt.tecnico.distledger.client.exceptions.ServerUnavailableException;


public class UserService extends Service {

    private Timestamp ts = new Timestamp();
    private int requestID = 0;

    public void createAccount(String server, String username)
            throws ServerLookupFailedException, ServerUnavailableException {
        if (!cacheHasServerEntry(server)) cacheRefresh(server);

        try {
            tryCreateAccount(server, username);
        } catch (ServerUnavailableException e) {
            cacheRefresh(server);
            tryCreateAccount(server, username);
        }
    }

    private void tryCreateAccount(String server, String username) throws ServerUnavailableException {
        ManagedChannel channel = getServerChannel(server);

        try{
            UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel);
            
            CreateAccountRequest request = CreateAccountRequest.newBuilder()
                                                                .setUserId(username)
                                                                .setPrev(ts)
                                                                .setUpdateId(username+"-"+ String.valueOf(requestID++))
                                                                .build();

            
            CreateAccountResponse response = stub.withDeadlineAfter(TIMEOUT, TimeUnit.MILLISECONDS).createAccount(request);
            ts.merge(Timestamp.fromGrpc(response.getTs()));
            
            System.out.println("OK");
            System.out.println(response);
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getDescription());
            System.err.println(e.getMessage());
            System.out.println("");

            if (e.getStatus() == Status.UNAVAILABLE) {
                channel.shutdown();
                throw new ServerUnavailableException(e);
            }
        }
    }

    
    public void deleteAccount(String server, String username)
    throws ServerLookupFailedException, ServerUnavailableException {
        if (!cacheHasServerEntry(server)) cacheRefresh(server);
        
        try {
            tryDeleteAccount(server, username);
        } catch (ServerUnavailableException e) {
            cacheRefresh(server);
            tryDeleteAccount(server, username);
        }
    }
    
    private void tryDeleteAccount(String server, String username) throws ServerUnavailableException {
        ManagedChannel channel = getServerChannel(server);
        
        try{
            UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel);
            
            DeleteAccountRequest request = DeleteAccountRequest.newBuilder()
                                                                .setUserId(username)
                                                                .setPrev(ts)
                                                                .setUpdateId(username+"-"+ String.valueOf(requestID++))
                                                                .build();

            DeleteAccountResponse response = stub.withDeadlineAfter(TIMEOUT, TimeUnit.MILLISECONDS).deleteAccount(request);
            merge(ts, response.getTs());

            System.out.println("OK");
            System.out.println(response);
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getDescription());
            System.err.println(e.getMessage());
            System.out.println("");
            
            if (e.getStatus() == Status.UNAVAILABLE) {
                channel.shutdown();
                throw new ServerUnavailableException(e);
            }
        }
    }
    
    public void balance(String server, String username) throws ServerUnavailableException, ServerLookupFailedException {
        if (!cacheHasServerEntry(server)) cacheRefresh(server);
        
        try {
            tryGetBalance(server, username);
        } catch (ServerUnavailableException e) {
            cacheRefresh(server);
            tryGetBalance(server, username);
        }
    }
    
    private void tryGetBalance(String server, String username) throws ServerUnavailableException {
        ManagedChannel channel = getServerChannel(server);
        
        try{
            UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel);
            
            BalanceRequest request = BalanceRequest.newBuilder()
                                                    .setUserId(username)
                                                    .setPrev(ts)
                                                    .build();

            BalanceResponse response = stub.withDeadlineAfter(TIMEOUT, TimeUnit.MILLISECONDS).balance(request);
            merge(ts, response.getTs());
            
            System.out.println("OK");
            System.out.println(response);
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getDescription());
            System.err.println(e.getMessage());
            System.out.println("");
            
            if (e.getStatus() == Status.UNAVAILABLE) {
                channel.shutdown();
                throw new ServerUnavailableException(e);
            }
        }
    }
    
    public void transferTo(String server, String username, String dest, Integer amount) 
    throws ServerLookupFailedException, ServerUnavailableException {
        if (!cacheHasServerEntry(server)) cacheRefresh(server);
        
        try {
            tryTransferTo(server, username, dest, amount);
        } catch (ServerUnavailableException e) {
            cacheRefresh(server);
            tryTransferTo(server, username, dest, amount);
        }
    }
    

    private void tryTransferTo(String server, String username, String dest, Integer amount) 
    throws ServerUnavailableException {
        ManagedChannel channel = getServerChannel(server);
        
        try{
            UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel);
            
            TransferToRequest request = TransferToRequest.newBuilder()
                                                            .setAccountFrom(username)
                                                            .setAccountTo(dest)
                                                            .setAmount(amount)
                                                            .setPrev(ts)
                                                            .setUpdateId(username+"-"+ String.valueOf(requestID++))
                                                            .build();

            TransferToResponse response = stub.withDeadlineAfter(TIMEOUT, TimeUnit.MILLISECONDS).transferTo(request);

            merge(ts, response.getTs());
            
            
            System.out.println("OK");
            System.out.println(response);
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getDescription());
            System.err.println(e.getMessage());
            System.out.println("");
            
            if (e.getStatus() == Status.UNAVAILABLE) {
                channel.shutdown();
                throw new ServerUnavailableException(e);
            }
        }
    }
    
    private ManagedChannel getServerChannel(String server) {
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(server).usePlaintext().build();
    
        return channel;
    }

//     private void merge(Timestamp ts1, Timestamp ts2) {     
//         for(int i: ts1.getNonNullReplicas()){
//             if (ts1.getTime(i) > ts2.getTime(i)) continue;
//             else ts1.updateTime(i, ts2.getTime(i));
//         }
//     }
}
