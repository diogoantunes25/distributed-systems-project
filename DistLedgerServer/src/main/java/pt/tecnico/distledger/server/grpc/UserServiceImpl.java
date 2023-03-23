package pt.tecnico.distledger.server.grpc;

import io.grpc.*;
import io.grpc.stub.StreamObserver;

import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.exceptions.*;
import pt.tecnico.distledger.server.exceptions.CannotPropagateStateException;
import pt.tecnico.distledger.server.exceptions.NoSecundaryServersException;

public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {

    
    private ServerState state;
    private CrossServerService crossServerService;

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
        this.crossServerService = new CrossServerService(state, qual);
    }

    @Override
    public void createAccount(CreateAccountRequest request, StreamObserver<CreateAccountResponse> responseObserver) {

        if (crossServerService.canWrite()) {
            try {
                String userID = request.getUserId();

                synchronized (this) {
                    state.assertCanCreateAccount(userID);
                    crossServerService.propagateState();
                    state.createAccount(userID);
                }

                CreateAccountResponse response = CreateAccountResponse.newBuilder().build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();

            } catch (AccountAlreadyExistsException e) {
                System.err.println(ACCOUNT_ALREADY_EXISTS);
                responseObserver.onError(Status.ALREADY_EXISTS.withDescription(ACCOUNT_ALREADY_EXISTS).asRuntimeException());

            } catch (ServerUnavailableException | NoSecundaryServersException | CannotPropagateStateException e) {
                System.err.println(SERVER_UNAVAILABLE);
                responseObserver.onError(Status.UNAVAILABLE.withDescription(SERVER_UNAVAILABLE).asRuntimeException());

            } catch (StatusRuntimeException e) {
                System.err.println(INTERNAL_ERROR);
                responseObserver.onError(Status.INTERNAL.withDescription(INTERNAL_ERROR).asRuntimeException());
            }
        } else {
            System.err.println(ONLY_PRIMARY_CAN_WRITE);
            responseObserver.onError(Status.PERMISSION_DENIED.withDescription(ONLY_PRIMARY_CAN_WRITE).asRuntimeException());
        }
    }

    @Override
    public void deleteAccount(DeleteAccountRequest request, StreamObserver<DeleteAccountResponse> responseObserver) {

        if (crossServerService.canWrite()) {
            try {
                String userID = request.getUserId();

                synchronized (this) {
                    state.assertCanDeleteAccount(userID);
                    crossServerService.propagateState();
                    state.deleteAccount(userID);
                }

                DeleteAccountResponse response = DeleteAccountResponse.newBuilder().build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();

            } catch (AccountDoesNotExistException e) {
                System.err.println(ACCOUNT_DOES_NOT_EXIST);
                responseObserver.onError(Status.NOT_FOUND.withDescription(ACCOUNT_DOES_NOT_EXIST).asRuntimeException());

            } catch (BalanceNotZeroException e) {
                System.err.println(BALANCE_NOT_ZERO);
                responseObserver.onError(Status.FAILED_PRECONDITION.withDescription(BALANCE_NOT_ZERO).asRuntimeException());

            } catch (BrokerCannotBeDeletedException e) {
                System.err.println(BROKER_CANNOT_BE_DELETED);
                responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(BROKER_CANNOT_BE_DELETED).asRuntimeException());

            } catch (ServerUnavailableException | NoSecundaryServersException | CannotPropagateStateException e) {
                System.err.println(SERVER_UNAVAILABLE);
                responseObserver.onError(Status.UNAVAILABLE.withDescription(SERVER_UNAVAILABLE).asRuntimeException());

            } catch (StatusRuntimeException e) {
                System.err.println(INTERNAL_ERROR);
                responseObserver.onError(Status.INTERNAL.withDescription(INTERNAL_ERROR).asRuntimeException());
            }
        } else {
            System.err.println(ONLY_PRIMARY_CAN_WRITE);
            responseObserver.onError(Status.PERMISSION_DENIED.withDescription(ONLY_PRIMARY_CAN_WRITE).asRuntimeException());
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

        } catch (AccountDoesNotExistException e) {
            System.err.println(ACCOUNT_DOES_NOT_EXIST);
            responseObserver.onError(Status.NOT_FOUND.withDescription(ACCOUNT_DOES_NOT_EXIST).asRuntimeException());

        } catch (ServerUnavailableException e) {
            System.err.println(SERVER_UNAVAILABLE);
            responseObserver.onError(Status.UNAVAILABLE.withDescription(SERVER_UNAVAILABLE).asRuntimeException());
        }

    }

    @Override
    public void transferTo(TransferToRequest request, StreamObserver<TransferToResponse> responseObserver) {

        if (crossServerService.canWrite()) {
            try {
                String userID = request.getAccountFrom();
                String dest = request.getAccountTo();
                Integer amount = request.getAmount();

                synchronized (this) {
                    state.assertCanTransferTo(userID, dest, amount);
                    crossServerService.propagateState();
                    state.transferTo(userID, dest, amount);
                }

                TransferToResponse response = TransferToResponse.newBuilder().build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();

            } catch (AccountDoesNotExistException e) {
                System.err.println(ACCOUNT_DOES_NOT_EXIST);
                responseObserver.onError(Status.NOT_FOUND.withDescription(ACCOUNT_DOES_NOT_EXIST).asRuntimeException());

            } catch (NotEnoughBalanceException e) {
                System.err.println(NOT_ENOUGH_BALANCE);
                responseObserver.onError(Status.FAILED_PRECONDITION.withDescription(NOT_ENOUGH_BALANCE).asRuntimeException());

            } catch (InvalidTransferAmountException e) {
                System.err.println(INVALID_TRANSFER_AMOUNT);
                responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(INVALID_TRANSFER_AMOUNT).asRuntimeException());

            } catch (ServerUnavailableException | NoSecundaryServersException | CannotPropagateStateException e) {
                System.err.println(SERVER_UNAVAILABLE);
                responseObserver.onError(Status.UNAVAILABLE.withDescription(SERVER_UNAVAILABLE).asRuntimeException());

            } catch (StatusRuntimeException e) {
                System.err.println(INTERNAL_ERROR);
                responseObserver.onError(Status.INTERNAL.withDescription(INTERNAL_ERROR).asRuntimeException());
            }
        } else {
            System.err.println(ONLY_PRIMARY_CAN_WRITE);
            responseObserver.onError(Status.PERMISSION_DENIED.withDescription(ONLY_PRIMARY_CAN_WRITE).asRuntimeException());
        }
    }
}
