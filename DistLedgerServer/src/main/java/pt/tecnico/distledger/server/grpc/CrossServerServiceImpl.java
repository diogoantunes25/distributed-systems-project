package pt.tecnico.distledger.server.grpc;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.exceptions.InvalidLedgerException;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.DeleteOp;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;

import java.util.List;

public class CrossServerServiceImpl extends DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceImplBase {

    final String INVALID_LEDGER_STATE = "Ledger state contains invalid operations";

    private ServerState state;

    public CrossServerServiceImpl(ServerState state) {
        this.state = state;
    }

    @Override
    public void propagateState(PropagateStateRequest request, StreamObserver<PropagateStateResponse> responseStreamObserver) {

        int ledgerSize = state.getLedgerState().size();
        if(request.getStart() != ledgerSize) {
            responseStreamObserver.onNext(PropagateStateResponse.newBuilder().setStart(ledgerSize).build());
            responseStreamObserver.onCompleted();
            return;
        }

        List<Operation> ledger = state.getLedgerState();

        request.getState().getLedgerList().forEach(op -> {
            switch (op.getType()) {
                case OP_TRANSFER_TO:
                    ledger.add(new TransferOp(op.getUserId(), op.getDestUserId(), op.getAmount()));
                    break;
                case OP_CREATE_ACCOUNT:
                    ledger.add(new CreateOp(op.getUserId()));
                    break;
                case OP_DELETE_ACCOUNT:
                    ledger.add(new DeleteOp(op.getUserId()));
                    break;
                default:
                    responseStreamObserver.onError(Status.INVALID_ARGUMENT.withDescription(INVALID_LEDGER_STATE).asRuntimeException());
            }
        });

        try {
            state.updateLedger(ledger);

            responseStreamObserver.onNext(PropagateStateResponse.newBuilder().setStart(ledger.size()).build());
            responseStreamObserver.onCompleted();
        } catch (InvalidLedgerException e) {
            responseStreamObserver.onError(Status.INVALID_ARGUMENT.withDescription(INVALID_LEDGER_STATE).asRuntimeException());
        }
    }
}
