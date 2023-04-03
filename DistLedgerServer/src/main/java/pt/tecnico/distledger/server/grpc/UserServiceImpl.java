package pt.tecnico.distledger.server.grpc;

import java.util.concurrent.locks.ReentrantLock;

import io.grpc.*;
import io.grpc.stub.StreamObserver;

import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.exceptions.*;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.DeleteOp;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.tecnico.distledger.server.exceptions.CannotPropagateStateException;
import pt.tecnico.distledger.server.exceptions.NoSecundaryServersException;

public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {

    
    private ServerState state;
    private CrossServerClient crossServerService;

    final String ACCOUNT_ALREADY_EXISTS = "This Account Already Exists";
    final String ACCOUNT_DOES_NOT_EXIST = "This Account Does Not Exist";
    final String NOT_ENOUGH_BALANCE = "Not Enough Balance";
    final String BALANCE_NOT_ZERO = "Balance Not Zero";
    final String BROKER_CANNOT_BE_DELETED = "Broker Cannot Be Deleted";
    final String SERVER_UNAVAILABLE = "Server Unavailable";
    final String INVALID_TRANSFER_AMOUNT = "Amount To Transfer Must Positive";
    final String ONLY_PRIMARY_CAN_WRITE = "Write Operations Can Only Be Executed By Primary Server";
    final String INTERNAL_ERROR = "Internal Error";


    public UserServiceImpl(ServerState state, String qual) {
        this.state = state;
        this.crossServerService = new CrossServerClient(state, qual);
    }

    @Override
    public void createAccount(CreateAccountRequest request, StreamObserver<CreateAccountResponse> responseObserver) {
        // TODO
    }

    @Override
    public void deleteAccount(DeleteAccountRequest request, StreamObserver<DeleteAccountResponse> responseObserver) {
        // TODO: throw unsupported error 
    }
    
    @Override
    public void balance(BalanceRequest request, StreamObserver<BalanceResponse> responseObserver) {
        // TODO
    }

    @Override
    public void transferTo(TransferToRequest request, StreamObserver<TransferToResponse> responseObserver) {
        // TODO
    }
}
