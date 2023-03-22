package pt.tecnico.distledger.server.grpc;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.grpc.*;
import io.grpc.stub.StreamObserver;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.exceptions.*;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.exceptions.DistLedgerDownException;
import pt.tecnico.distledger.server.exceptions.CannotGetServerAddressesException;
import pt.tecnico.distledger.server.visitor.MessageConverterVisitor;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.LedgerState;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;

public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {
    
    private final String SERVICE_NAME = "DistLedger";

    private ServerState state;

    private final String qual;
    private static final String PRIMARY_QUAL = "A";
    private static final String SECONDARY_QUAL = "B";

    private Map<String, Integer> secondaryServerCache;

    final String ACCOUNT_ALREADY_EXISTS = "This Account Already Exists";
    final String ACCOUNT_DOES_NOT_EXIST = "This Account Does Not Exist";
    final String NOT_ENOUGH_BALANCE = "Not Enough Balance";
    final String BALANCE_NOT_ZERO = "Balance Not Zero";
    final String BROKER_CANNOT_BE_DELETED = "Broker Cannot Be Deleted";
    final String SERVER_UNAVAILABLE = "Server Unavailable";
    final String INVALID_TRANSFER_AMOUNT = "Amount to transfer must positive";
    final String ONLY_PRIMARY_CAN_WRITE = "Write operations can only be executed by primary server";
    final String LEDGER_DOWN = "Ledger is currently unavailable for write operations";

    public UserServiceImpl(ServerState state, String qual) {
        this.state = state;
        this.qual = qual;
        this.secondaryServerCache = new HashMap<>();
    }

    public boolean canWrite() {
        return qual.equals(PRIMARY_QUAL);
    }

    @Override
    public void createAccount(CreateAccountRequest request, StreamObserver<CreateAccountResponse> responseObserver) {

        if (canWrite()) {
            try {
                String userID = request.getUserId();

                synchronized (this) {
                    state.assertCanCreateAccount(userID);
                    propagateState();
                    state.createAccount(userID);
                }

                CreateAccountResponse response = CreateAccountResponse.newBuilder().build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();

            } catch (AccountAlreadyExistsException e) {
                System.err.println(ACCOUNT_ALREADY_EXISTS);
                responseObserver.onError(Status.ALREADY_EXISTS.withDescription(ACCOUNT_ALREADY_EXISTS).asRuntimeException());

            } catch (InterruptedException | ServerUnavailableException e) {
                System.err.println(SERVER_UNAVAILABLE);
                responseObserver.onError(Status.UNAVAILABLE.withDescription(SERVER_UNAVAILABLE).asRuntimeException());

            } catch (CannotGetServerAddressesException | DistLedgerDownException e) {
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

                synchronized (this) {
                    state.assertCanDeleteAccount(userID);
                    propagateState();
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

            } catch (InterruptedException | ServerUnavailableException e) {
                System.err.println(SERVER_UNAVAILABLE);
                responseObserver.onError(Status.UNAVAILABLE.withDescription(SERVER_UNAVAILABLE).asRuntimeException());

            } catch (CannotGetServerAddressesException | DistLedgerDownException e) {
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

                synchronized (this) {
                    state.assertCanTransferTo(userID, dest, amount);
                    propagateState();
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

            } catch (InterruptedException | ServerUnavailableException e) {
                System.err.println(SERVER_UNAVAILABLE);
                responseObserver.onError(Status.UNAVAILABLE.withDescription(SERVER_UNAVAILABLE).asRuntimeException());

            } catch (InvalidTransferAmountException e) {
                System.err.println(INVALID_TRANSFER_AMOUNT);
                responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(INVALID_TRANSFER_AMOUNT).asRuntimeException());

            } catch (CannotGetServerAddressesException | DistLedgerDownException e) {
                System.err.println(LEDGER_DOWN);
                responseObserver.onError(Status.UNAVAILABLE.withDescription(LEDGER_DOWN).asRuntimeException());
            }
        } else {
            System.err.println(ONLY_PRIMARY_CAN_WRITE);
            responseObserver.onError(Status.PERMISSION_DENIED.withDescription(ONLY_PRIMARY_CAN_WRITE).asRuntimeException());
        }
    }

    private void propagateStateToServer(String server, Integer ledgerSize) throws DistLedgerDownException {
        ManagedChannel channel = ManagedChannelBuilder.forTarget(server).usePlaintext().build();
        DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub stub = DistLedgerCrossServerServiceGrpc.newBlockingStub(channel);

        MessageConverterVisitor visitor = new MessageConverterVisitor();
        LedgerState.Builder ledgerStateBuilder = LedgerState.newBuilder();
        List<Operation> ledgerState = state.getLedgerState();
        ledgerState.subList(ledgerSize, ledgerState.size()).forEach(o -> ledgerStateBuilder.addLedger(o.accept(visitor)));

        PropagateStateRequest propagateStateRequest = PropagateStateRequest.newBuilder()
                .setStart(ledgerSize)
                .setState(ledgerStateBuilder.build())
                .build();

        PropagateStateResponse response = stub.propagateState(propagateStateRequest);

        // If secondary server had unexpected state, send missing operations
        if(response.getStart() != ledgerState.size()) {
            LedgerState.Builder newLedgerStateBuilder = LedgerState.newBuilder();
            ledgerState.subList(response.getStart(), ledgerState.size()).forEach(o -> newLedgerStateBuilder.addLedger(o.accept(visitor)));

            propagateStateRequest = PropagateStateRequest.newBuilder()
                    .setStart(response.getStart())
                    .setState(newLedgerStateBuilder.build())
                    .build();

            stub.propagateState(propagateStateRequest);
        }

        secondaryServerCache.replace(server, response.getStart());

        channel.shutdown();
    }

    private boolean broadcastState() 
            throws CannotGetServerAddressesException, DistLedgerDownException, InterruptedException {
        int secondaryHostsNum = this.secondaryServerCache.size();
        if (secondaryHostsNum == 0) {
            throw new DistLedgerDownException();
        }

        CountDownLatch latch = new CountDownLatch(1);
        for(Map.Entry<String, Integer> entry : this.secondaryServerCache.entrySet()){
            new Thread(() -> {
                try {
                    propagateStateToServer(entry.getKey(), entry.getValue());
                    latch.countDown();
                } catch (DistLedgerDownException e){}
            }).start();
        }

        // TODO: How much should the timeout be?
        return latch.await(1, TimeUnit.SECONDS);
    }

    private void cacheRefresh() throws CannotGetServerAddressesException, DistLedgerDownException {
        Map<String, Integer> newCache = new HashMap<>();
        for(String secondaryHost : NamingServerServiceImpl.getServerAddresses(SERVICE_NAME, SECONDARY_QUAL)){
            if(this.secondaryServerCache.containsKey(secondaryHost)){
                newCache.put(secondaryHost, this.secondaryServerCache.get(secondaryHost));
            } else {
                newCache.put(secondaryHost, 0);
            }
        }
        this.secondaryServerCache = newCache;
    }

    private void propagateState() 
            throws CannotGetServerAddressesException, DistLedgerDownException, InterruptedException {
        if(broadcastState())
            return;
            
        cacheRefresh();
        if(broadcastState())
            return;

        throw new DistLedgerDownException();
    }
}
