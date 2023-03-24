package pt.tecnico.distledger.userclient.grpc;

import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.*;
import pt.tecnico.distledger.client.grpc.Service;

import pt.tecnico.distledger.client.exceptions.ServerLookupFailedException;
import pt.tecnico.distledger.client.exceptions.ServerUnavailableException;

public class UserService extends Service {

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
                    .build();

            CreateAccountResponse response = stub.withDeadlineAfter(TIMEOUT, TimeUnit.MILLISECONDS).createAccount(request);

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
                    .build();

            DeleteAccountResponse response = stub.withDeadlineAfter(TIMEOUT, TimeUnit.MILLISECONDS).deleteAccount(request);

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
                    .build();

            BalanceResponse response = stub.withDeadlineAfter(TIMEOUT, TimeUnit.MILLISECONDS).balance(request);

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

    public void transferTo(String server, String username, String dest, Integer amount) throws ServerLookupFailedException, ServerUnavailableException {
        if (!cacheHasServerEntry(server)) cacheRefresh(server);

        try {
            tryTransferTo(server, username, dest, amount);
        } catch (ServerUnavailableException e) {
            cacheRefresh(server);
            tryTransferTo(server, username, dest, amount);
        }
    }

    private void tryTransferTo(String server, String username, String dest, Integer amount) throws ServerUnavailableException {
        ManagedChannel channel = getServerChannel(server);

        try{
            UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel);
            TransferToRequest request = TransferToRequest.newBuilder()
                    .setAccountFrom(username)
                    .setAccountTo(dest)
                    .setAmount(amount)
                    .build();

            TransferToResponse response = stub.withDeadlineAfter(TIMEOUT, TimeUnit.MILLISECONDS).transferTo(request);
            
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

}
