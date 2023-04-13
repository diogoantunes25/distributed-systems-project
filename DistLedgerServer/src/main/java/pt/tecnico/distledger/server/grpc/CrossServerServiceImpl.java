package pt.tecnico.distledger.server.grpc;

import java.util.ArrayList;
import java.util.List;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import pt.tecnico.distledger.server.domain.operation.*;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;

import pt.tecnico.distledger.server.domain.UpdateId;
import pt.tecnico.distledger.gossip.Timestamp;
import pt.tecnico.distledger.server.domain.ServerState;

public class CrossServerServiceImpl extends DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceImplBase {

    final String INVALID_LEDGER_STATE = "Ledger State Contains Invalid Operations";

    private ServerState state;

    public CrossServerServiceImpl(ServerState state) {
        this.state = state;
    }

    @Override
    public void propagateState(PropagateStateRequest request, StreamObserver<PropagateStateResponse> responseStreamObserver) {
        System.out.printf("[CrossServerServiceImpl] propagateState request received\n");
        List<UpdateOp> receivedOps = new ArrayList<>();

        request.getLog().getLedgerList().forEach(op -> {
            switch (op.getType()) {
                case OP_TRANSFER_TO:
                receivedOps.add(new TransferOp(
                    Timestamp.fromGrpc(op.getPrev()),
                    Timestamp.fromGrpc(op.getTs()),
                    new UpdateId(op.getUpdateId()),
                    op.getUserId(),
                    op.getDestUserId(),
                    op.getAmount()
                ));
                break;
            case OP_CREATE_ACCOUNT:
                receivedOps.add(new CreateOp(
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


        System.out.printf("[CrossServerServiceImpl] merging log of size %s\n", receivedOps.size());
        state.mergeLog(receivedOps, Timestamp.fromGrpc(request.getReplicaTS()));

        responseStreamObserver.onNext(PropagateStateResponse.newBuilder().build());
        responseStreamObserver.onCompleted();
    }
}
