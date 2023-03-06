package pt.tecnico.distledger.server.grpc;

import io.grpc.stub.StreamObserver;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;



public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {

    ServerState state = new ServerState();

    @Override
    public void createAccount(CreateAccountRequest request, StreamObserver<CreateAccountResponse> responseObserver) {
        
        String userID = request.getUserId();
        state.createAccount(userID);
        CreateAccountResponse response = CreateAccountResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void deleteAccount(DeleteAccountRequest request, StreamObserver<DeleteAccountResponse> responseObserver) {
        String userID = request.getUserId();
        state.deleteAccount(userID);
        DeleteAccountResponse response = DeleteAccountResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
    
    @Override
    public void balance(BalanceRequest request, StreamObserver<BalanceResponse> responseObserver) {
        String userID = request.getUserId();
        BalanceResponse response = BalanceResponse.newBuilder().setValue(state.getBalance(userID)).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void transferTo(TransferToRequest request, StreamObserver<TransferToResponse> responseObserver) {
        String userID = request.getAccountFrom();
        String dest = request.getAccountTo();
        Integer amount = request.getAmount();
        state.transferTo(userID, dest, amount);
        TransferToResponse response = TransferToResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    
}
