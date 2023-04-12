package pt.tecnico.distledger.server.grpc;

import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import pt.tecnico.distledger.server.domain.operation.*;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;

import pt.tecnico.distledger.server.domain.exceptions.InvalidLedgerException;
import pt.tecnico.distledger.server.domain.exceptions.ServerUnavailableException;
import pt.tecnico.distledger.server.exceptions.OperationAlreadyExecutedException;
import pt.tecnico.distledger.server.domain.UpdateId;
import pt.tecnico.distledger.gossip.Timestamp;
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
        System.out.printf("[CrossServerServiceImple] propagateState request received\n");
        List<UpdateOp> receveidOps = new ArrayList<>();

        request.getLog().getLedgerList().forEach(op -> {
            switch (op.getType()) {
                case OP_TRANSFER_TO:
                receveidOps.add(new TransferOp(
                    Timestamp.fromGrpc(op.getPrev()),
                    Timestamp.fromGrpc(op.getTs()),
                    new UpdateId(op.getUpdateId()),
                    op.getUserId(),
                    op.getDestUserId(),
                    op.getAmount()
                ));
                break;
            case OP_CREATE_ACCOUNT:
                receveidOps.add(new CreateOp(
                    Timestamp.fromGrpc(op.getPrev()),
                    Timestamp.fromGrpc(op.getTs()),
                    new UpdateId(op.getUpdateId()),
                    op.getUserId()
                ));
                break;
            case OP_DELETE_ACCOUNT:
                receveidOps.add(new DeleteOp(
                    Timestamp.fromGrpc(op.getPrev()),
                    Timestamp.fromGrpc(op.getTs()),
                    new UpdateId(op.getUpdateId()),
                    op.getUserId()
                ));
                break;
            default:
                responseStreamObserver.onError(Status.INVALID_ARGUMENT.withDescription(INVALID_LEDGER_STATE).asRuntimeException());
            }
        });


        System.out.printf("[CrossServerServiceImple] merging log\n");
        state.mergeLog(receveidOps, Timestamp.fromGrpc(request.getReplicaTS()));

        responseStreamObserver.onNext(PropagateStateResponse.newBuilder().build());
        responseStreamObserver.onCompleted();
    }
}
