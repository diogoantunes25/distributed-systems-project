package pt.tecnico.distledger.server.grpc;

import java.util.concurrent.locks.ReentrantLock;

import io.grpc.*;
import io.grpc.stub.StreamObserver;

import pt.tecnico.distledger.gossip.Timestamp;
import pt.tecnico.distledger.server.domain.UpdateId;
import pt.tecnico.distledger.server.domain.exceptions.AccountDoesNotExistException;
import pt.tecnico.distledger.server.domain.exceptions.DistLedgerRuntimeException;
import pt.tecnico.distledger.server.exceptions.OperationAlreadyExecutedException;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;

import pt.tecnico.distledger.server.domain.ServerState;

public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {

    
    private ServerState state;
    private CrossServerClient crossServerService;

    final String ACCOUNT_DOES_NOT_EXIST = "Account does not exist";
    final String DELETE_UNAVAILABLE = "Delete operations are not allowed";
    final String UPDATE_ALREADY_PROCESSED = "Update with provided uid was already processed";


    public UserServiceImpl(ServerState state, String qual) {
        this.state = state;
        this.crossServerService = new CrossServerClient(state, qual);
    }

    @Override
    public void createAccount(CreateAccountRequest request, StreamObserver<CreateAccountResponse> responseObserver) {
        try {
            Timestamp newTS = state.createAccount(new UpdateId(request.getUpdateId()), request.getUserId(),
                    Timestamp.fromGrpc(request.getPrev()));

            CreateAccountResponse response = CreateAccountResponse.newBuilder()
                    .setTs(newTS.toGrpc())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (OperationAlreadyExecutedException e) {
            e.printStackTrace();
            responseObserver.onError(Status.CANCELLED.withDescription(UPDATE_ALREADY_PROCESSED).asRuntimeException());
        }
    }

    @Override
    public void deleteAccount(DeleteAccountRequest request, StreamObserver<DeleteAccountResponse> responseObserver) {
        responseObserver.onError(Status.UNAVAILABLE.withDescription(DELETE_UNAVAILABLE).asRuntimeException());
    }
    
    @Override
    public void balance(BalanceRequest request, StreamObserver<BalanceResponse> responseObserver) {
        try {
            ServerState.Read<Integer> read = state.getBalance(request.getUserId(), Timestamp.fromGrpc(request.getPrev()));

            BalanceResponse response = BalanceResponse.newBuilder()
                    .setValue(read.getValue())
                    .setNew(read.getNewTs().toGrpc())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (DistLedgerRuntimeException e) {
            e.printStackTrace();
            responseObserver.onError(Status.FAILED_PRECONDITION.withDescription(ACCOUNT_DOES_NOT_EXIST).asRuntimeException());
        }
    }

    @Override
    public void transferTo(TransferToRequest request, StreamObserver<TransferToResponse> responseObserver) {
        try {
            Timestamp newTS = state.transferTo(new UpdateId(request.getUpdateId()), request.getAccountFrom(),
                    request.getAccountTo(), request.getAmount(), Timestamp.fromGrpc(request.getPrev()));

            TransferToResponse response = TransferToResponse.newBuilder()
                    .setTs(newTS.toGrpc())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (OperationAlreadyExecutedException e) {
            e.printStackTrace();
            responseObserver.onError(Status.CANCELLED.withDescription(UPDATE_ALREADY_PROCESSED).asRuntimeException());
        }
    }
}
