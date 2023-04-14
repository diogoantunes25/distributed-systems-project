package pt.tecnico.distledger.userclient.grpc;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import pt.tecnico.distledger.gossip.Timestamp;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.*;
import pt.tecnico.distledger.client.grpc.Service;

import pt.tecnico.distledger.client.exceptions.ServerLookupFailedException;
import pt.tecnico.distledger.client.exceptions.ServerUnavailableException;

public class UserService extends Service {

    private Timestamp ts = new Timestamp();
    private int requestID = 0;

    public void createAccount(String server, String username)
            throws ServerLookupFailedException, ServerUnavailableException {
        try {
            tryCreateAccount(server, username);
        } catch (ServerUnavailableException e) {
            cacheRefresh(server);
            tryCreateAccount(server, username);
        }
    }

    private void tryCreateAccount(String server, String username) 
            throws ServerUnavailableException {
        ManagedChannel channel = getServerChannel(server);

        try{
            UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc
                .newBlockingStub(channel);
            
            CreateAccountRequest request = CreateAccountRequest.newBuilder()
                                                               .setUserId(username)
                                                               .setPrev(ts.toGrpc())
                                                               .setUpdateId(getId() + "-" + requestID++)
                                                               .build();
            
            CreateAccountResponse response = stub
                .withDeadlineAfter(SHORT_TIMEOUT, TimeUnit.MILLISECONDS)
                .createAccount(request);
            ts.merge(Timestamp.fromGrpc(response.getTs()));
            
            System.out.println("OK");
            System.out.println(response);
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getDescription());
            System.err.println(e.getMessage());
            System.out.println("");

            if (Status.UNAVAILABLE.getCode() == e.getStatus().getCode()) {
                System.out.printf("[UserService] server unavailable\n");
                removeServer(server);
                throw new ServerUnavailableException(e);
            }
        }
    }

    public void balance(String server, String username) 
            throws ServerUnavailableException, ServerLookupFailedException {
        try {
            tryGetBalance(server, username);
        } catch (ServerUnavailableException e) {
            cacheRefresh(server);
            tryGetBalance(server, username);
        }
    }
    
    private void tryGetBalance(String server, String username) 
            throws ServerUnavailableException {
        ManagedChannel channel = getServerChannel(server);
        System.out.printf("Got channel for %s\n", server);
        
        try{
            UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc
                .newBlockingStub(channel);
            System.out.printf("Got stub for %s\n", server);

            BalanceRequest request = BalanceRequest.newBuilder()
                                                   .setUserId(username)
                                                   .setPrev(ts.toGrpc())
                                                   .build();

            // long timeout is needed because read operations block until stable
            System.out.printf("Sending request\n", server);
            BalanceResponse response = stub
                .withDeadlineAfter(LONG_TIMEOUT, TimeUnit.MILLISECONDS)
                .balance(request);
            ts.merge(Timestamp.fromGrpc(response.getNew()));

            System.out.println("OK");
            System.out.println(response);
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getDescription());
            System.err.println(e.getMessage());
            System.out.println("");
            
            if (e.getStatus().getCode() == Status.UNAVAILABLE.getCode()) {
                removeServer(server);
                throw new ServerUnavailableException(e);
            }
        }
    }
    
    public void transferTo(String server, String username, String dest, Integer amount) 
            throws ServerLookupFailedException, ServerUnavailableException {
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
            UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc
                .newBlockingStub(channel);
            
            TransferToRequest request = TransferToRequest.newBuilder()
                                                         .setAccountFrom(username)
                                                         .setAccountTo(dest)
                                                         .setAmount(amount)
                                                         .setPrev(ts.toGrpc())
                                                         .setUpdateId(getId() + "-" + requestID++)
                                                         .build();

            TransferToResponse response = stub
                .withDeadlineAfter(SHORT_TIMEOUT, TimeUnit.MILLISECONDS)
                .transferTo(request);
            ts.merge(Timestamp.fromGrpc(response.getTs()));

            System.out.println("OK");
            System.out.println(response);
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getDescription());
            System.err.println(e.getMessage());
            System.out.println("");
            
            if (e.getStatus().getCode() == Status.UNAVAILABLE.getCode()) {
                removeServer(server);
                throw new ServerUnavailableException(e);
            }
        }
    }
}
