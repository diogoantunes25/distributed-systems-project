package pt.tecnico.distledger.server.grpc;

import io.grpc.stub.StreamObserver;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;



public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {

    ServerState state = new ServerState();

    @Override
    public void createAccount(CreateAccountRequest request, StreamObserver<CreateAccountResponse> responseObserver) {
        
        String username = request.getUserId();
        state.createAccount(username);
        CreateAccountResponse response = CreateAccountResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
}

    @Override
    public void deleteAccount(DeleteAccountRequest request, StreamObserver<DeleteAccountResponse> responseObserver) {
        String username = request.getUserId();
        state.createAccount(username);
        DeleteAccountResponse response = DeleteAccountResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
    
    @Override
    public void balance(BalanceRequest request, StreamObserver<BalanceResponse> responseObserver) {
        String username = request.getUserId();
        state.getBalance(username);
        BalanceResponse response = BalanceResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void transferTo(TransferToRequest request, StreamObserver<TransferToResponse> responseObserver) {
        String username = request.getAccountFrom();
        String dest = request.getAccountTo();
        Integer amount = request.getAmount();
        state.transferTo(username, dest, amount);
        TransferToResponse response = TransferToResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    
}
