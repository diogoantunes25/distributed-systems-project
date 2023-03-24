package pt.tecnico.distledger.server.grpc;

import java.util.ArrayList;
import java.util.List;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;

import pt.tecnico.distledger.server.domain.exceptions.InvalidLedgerException;
import pt.tecnico.distledger.server.domain.exceptions.ServerUnavailableException;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.DeleteOp;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.tecnico.distledger.server.domain.ServerState;

public class CrossServerServiceImpl extends DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceImplBase {

    final String INVALID_LEDGER_STATE = "Ledger State Contains Invalid Operations";
    final String SERVER_UNAVAILABLE = "Server Is Unavailable";

    private ServerState state;

    public CrossServerServiceImpl(ServerState state) {
        this.state = state;
    }

    @Override
    public void propagateState(PropagateStateRequest request, StreamObserver<PropagateStateResponse> responseStreamObserver) {
        List<Operation> newOperations = new ArrayList<>();

        request.getState().getLedgerList().forEach(op -> {
            switch (op.getType()) {
                case OP_TRANSFER_TO:
                    newOperations.add(new TransferOp(op.getUserId(), op.getDestUserId(), op.getAmount()));
                    break;
                case OP_CREATE_ACCOUNT:
                    newOperations.add(new CreateOp(op.getUserId()));
                    break;
                case OP_DELETE_ACCOUNT:
                    newOperations.add(new DeleteOp(op.getUserId()));
                    break;
                default:
                    responseStreamObserver.onError(Status.INVALID_ARGUMENT.withDescription(INVALID_LEDGER_STATE).asRuntimeException());
            }
        });

        try {
            state.updateLedger(newOperations, request.getStart());

            responseStreamObserver.onNext(PropagateStateResponse.newBuilder().build());
            responseStreamObserver.onCompleted();
        } catch (InvalidLedgerException e) {
            responseStreamObserver.onError(Status.INVALID_ARGUMENT.withDescription(INVALID_LEDGER_STATE).asRuntimeException());
        } catch (ServerUnavailableException e) {
            responseStreamObserver.onError(Status.UNAVAILABLE.withDescription(SERVER_UNAVAILABLE).asRuntimeException());
        }
    }
}
