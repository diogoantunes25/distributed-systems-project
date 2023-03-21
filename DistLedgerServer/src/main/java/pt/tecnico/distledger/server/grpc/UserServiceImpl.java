package pt.tecnico.distledger.server.grpc;

import io.grpc.*;
import io.grpc.stub.StreamObserver;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.exceptions.*;
import pt.tecnico.distledger.server.exceptions.DistLedgerDownException;
import pt.tecnico.distledger.server.exceptions.ServerRegistrationFailedException;
import pt.tecnico.distledger.server.exceptions.ServerUnregistrationFailedException;
import pt.tecnico.distledger.server.visitor.MessageConverterVisitor;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.NamingServerDistLedger;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.NamingServerDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.NamingServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;



public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {

    private ServerState state;
    private final String qual;
    private static final String PRIMARY_QUAL = "A";
    private static final String SECONDARY_QUAL = "B";

    private final ManagedChannel nameServerChannel;
    private final NamingServiceGrpc.NamingServiceBlockingStub nameServerStub;
    private final static String SERVICE_NAME = "UserService";

    final String ACCOUNT_ALREADY_EXISTS = "This Account Already Exists";
    final String ACCOUNT_DOES_NOT_EXIST = "This Account Does Not Exist";
    final String NOT_ENOUGH_BALANCE = "Not Enough Balance";
    final String BALANCE_NOT_ZERO = "Balance Not Zero";
    final String BROKER_CANNOT_BE_DELETED = "Broker Cannot Be Deleted";
    final String SERVER_UNAVAILABLE = "Server Unavailable";
    final String INVALID_TRANSFER_AMOUNT = "Amount to transfer must positive";
    final String ONLY_PRIMARY_CAN_WRITE = "Write operations can only be executed by primary server";
    final String LEDGER_DOWN = "Ledger is currently unavailable for write operations";

    public UserServiceImpl(ServerState state, String qual, String nameServer) {
        this.state = state;
        this.qual = qual;

        this.nameServerChannel = ManagedChannelBuilder.forTarget(nameServer).usePlaintext().build();
        this.nameServerStub = NamingServiceGrpc.newBlockingStub(nameServerChannel);
    }

    public void register(String address) throws ServerRegistrationFailedException {
        try {
            NamingServerDistLedger.RegisterRequest request = NamingServerDistLedger.RegisterRequest
                    .newBuilder()
                    .setServiceName(SERVICE_NAME)
                    .setQualifier(qual)
                    .setAddress(address)
                    .build();

            NamingServerDistLedger.RegisterResponse response = nameServerStub.register(request);
        } catch (StatusRuntimeException e) {
            e.printStackTrace();
            throw new ServerRegistrationFailedException(address, qual, SERVICE_NAME, e);
        }
    }

    public void unregister(String address) throws ServerUnregistrationFailedException {
        try {
            NamingServerDistLedger.DeleteRequest request = NamingServerDistLedger.DeleteRequest
                    .newBuilder()
                    .setServiceName(SERVICE_NAME)
                    .setHostname(address)
                    .build();

            nameServerStub.delete(request);
        } catch (StatusRuntimeException e) {
            e.printStackTrace();
            throw new ServerUnregistrationFailedException(address, qual, SERVICE_NAME, e);
        }
    }

    public boolean canWrite() {
        return qual.equals(PRIMARY_QUAL);
    }

    @Override
    public void createAccount(CreateAccountRequest request, StreamObserver<CreateAccountResponse> responseObserver) {

        if (canWrite()) {
            try {
                String userID = request.getUserId();
                state.createAccount(userID);

                propagateState();

                CreateAccountResponse response = CreateAccountResponse.newBuilder().build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } catch (AccountAlreadyExistsException e) {
                System.err.println(ACCOUNT_ALREADY_EXISTS);
                responseObserver.onError(Status.ALREADY_EXISTS.withDescription(ACCOUNT_ALREADY_EXISTS).asRuntimeException());

            } catch (ServerUnavailableException e) {
                System.err.println(SERVER_UNAVAILABLE);
                responseObserver.onError(Status.UNAVAILABLE.withDescription(SERVER_UNAVAILABLE).asRuntimeException());
            } catch (DistLedgerDownException e) {
                System.err.println(LEDGER_DOWN);
                responseObserver.onError(Status.UNAVAILABLE.withDescription(LEDGER_DOWN).asRuntimeException());
            }
        } else {
            System.err.println(ONLY_PRIMARY_CAN_WRITE);
            responseObserver.onError(Status.PERMISSION_DENIED.withDescription(ONLY_PRIMARY_CAN_WRITE).asRuntimeException());
        }
    }

    @Override
    public void deleteAccount(DeleteAccountRequest request, StreamObserver<DeleteAccountResponse> responseObserver) {

        if (canWrite()) {
            try {
                String userID = request.getUserId();
                state.deleteAccount(userID);

                propagateState();

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

            } catch (ServerUnavailableException e) {
                System.err.println(SERVER_UNAVAILABLE);
                responseObserver.onError(Status.UNAVAILABLE.withDescription(SERVER_UNAVAILABLE).asRuntimeException());
            } catch (DistLedgerDownException e) {
                System.err.println(LEDGER_DOWN);
                responseObserver.onError(Status.UNAVAILABLE.withDescription(LEDGER_DOWN).asRuntimeException());
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

        if (canWrite()) {
            try {
                String userID = request.getAccountFrom();
                String dest = request.getAccountTo();
                Integer amount = request.getAmount();

                propagateState();

                state.transferTo(userID, dest, amount);
                TransferToResponse response = TransferToResponse.newBuilder().build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();

            } catch (AccountDoesNotExistException e) {
                System.err.println(ACCOUNT_DOES_NOT_EXIST);
                responseObserver.onError(Status.NOT_FOUND.withDescription(ACCOUNT_DOES_NOT_EXIST).asRuntimeException());

            } catch (NotEnoughBalanceException e) {
                System.err.println(NOT_ENOUGH_BALANCE);
                responseObserver.onError(Status.FAILED_PRECONDITION.withDescription(NOT_ENOUGH_BALANCE).asRuntimeException());

            } catch (ServerUnavailableException e) {
                System.err.println(SERVER_UNAVAILABLE);
                responseObserver.onError(Status.UNAVAILABLE.withDescription(SERVER_UNAVAILABLE).asRuntimeException());
            } catch (InvalidTransferAmountException e) {
                System.err.println(INVALID_TRANSFER_AMOUNT);
                responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(INVALID_TRANSFER_AMOUNT).asRuntimeException());
            } catch (DistLedgerDownException e) {
                System.err.println(LEDGER_DOWN);
                responseObserver.onError(Status.UNAVAILABLE.withDescription(LEDGER_DOWN).asRuntimeException());
            }
        } else {
            System.err.println(ONLY_PRIMARY_CAN_WRITE);
            responseObserver.onError(Status.PERMISSION_DENIED.withDescription(ONLY_PRIMARY_CAN_WRITE).asRuntimeException());
        }
    }

    public void propagateState() throws DistLedgerDownException {
        LookupRequest request = LookupRequest.newBuilder()
                .setQualifier(SECONDARY_QUAL)
                .setServicename(SERVICE_NAME)
                .build();

        LookupResponse response = nameServerStub.lookup(request);
        if (response.getServicesCount() == 0) {
            throw new DistLedgerDownException();
        }

        String secondaryHost = response.getServices(0);
        ManagedChannel channel = ManagedChannelBuilder.forTarget(secondaryHost).usePlaintext().build();
        try {
            DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub stub = DistLedgerCrossServerServiceGrpc.newBlockingStub(channel);

            DistLedgerCommonDefinitions.LedgerState.Builder ledgerStateBuilder = DistLedgerCommonDefinitions.LedgerState.newBuilder();
            MessageConverterVisitor visitor = new MessageConverterVisitor();
            state.getLedgerState().forEach(o -> ledgerStateBuilder.addLedger(o.accept(visitor)));

            PropagateStateRequest propagateStateRequest = PropagateStateRequest.newBuilder()
                    .setState(ledgerStateBuilder.build())
                    .build();

            stub.propagateState(propagateStateRequest);
        } catch (StatusRuntimeException e) {
            e.printStackTrace();
            throw new DistLedgerDownException(e);
        } finally {
            channel.shutdown();
        }
    }
}
