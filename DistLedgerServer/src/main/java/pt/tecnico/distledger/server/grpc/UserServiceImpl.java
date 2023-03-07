package pt.tecnico.distledger.server.grpc;

import io.grpc.stub.StreamObserver;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;



public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {

    ServerState state = new ServerState();

    @Override
    public void createAccount(CreateAccountRequest request, StreamObserver<CreateAccountResponse> responseObserver) {
        
        try {
            String userID = request.getUserId();
            state.createAccount(userID);

            CreateAccountResponse response = CreateAccountResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (ExceptionAccountAlreadyExistsException | ServerUnavailableException e) {
            System.err.println(e.getMessage());
            responseObserver.onError(e);
        }
    }

    @Override
    public void deleteAccount(DeleteAccountRequest request, StreamObserver<DeleteAccountResponse> responseObserver) {

        try {
            String userID = request.getUserId();
            state.deleteAccount(userID);

            DeleteAccountResponse response = DeleteAccountResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (AccountDoesNotExistException | BalanceNotZeroException | ServerUnavailableException | BrokerCannotBeDeletedException e) {
            System.err.println(e.getMessage());
            responseObserver.onError(e);
        }
    }
    
    @Override
    public void balance(BalanceRequest request, StreamObserver<BalanceResponse> responseObserver) {

        try {
            String userID = request.getUserId();
            Integer balance = state.getBalance(userID);

            BalanceResponse response = BalanceResponse.newBuilder().setValue(balance).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (AccountDoesNotExistException | ServerUnavailableException e) {
            System.err.println(e.getMessage());
            responseObserver.onError(e);
        }

    }

    @Override
    public void transferTo(TransferToRequest request, StreamObserver<TransferToResponse> responseObserver) {

        try {
            String userID = request.getAccountFrom();
            String dest = request.getAccountTo();
            Integer amount = request.getAmount();

            state.transferTo(userID, dest, amount);
            TransferToResponse response = TransferToResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (AccountDoesNotExistException | NotEnoughBalanceException | ServerUnavailableException e) {
            System.err.println(e.getMessage());
            responseObserver.onError(e);
        }
    }

    
}
