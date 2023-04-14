package pt.tecnico.distledger.server.grpc;

import java.util.ArrayList;
import java.util.List;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import pt.tecnico.distledger.server.domain.operation.*;
import pt.tecnico.distledger.server.exceptions.NoReplicasException;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;

import pt.tecnico.distledger.server.domain.UpdateId;
import pt.tecnico.distledger.server.domain.exceptions.ServerUnavailableException;
import pt.tecnico.distledger.gossip.Timestamp;
import pt.tecnico.distledger.server.domain.ServerState;

public class CrossServerServiceImpl extends DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceImplBase {

    final String INVALID_LEDGER_STATE = "Ledger State Contains Invalid Operations";
    final String NO_REPLICAS = "No Replicas Found.";
    final String SERVER_UNAVAILABLE = "Server Unavailable.";

    private ServerState state;
    private CrossServerClient crossServerClient;

    public CrossServerServiceImpl(ServerState state, CrossServerClient client) {
        this.state = state;
        this.crossServerClient = client;
    }

    @Override
    public void invalidateCache(InvalidateServerCacheRequest request, StreamObserver<InvalidateServerCacheResponse> responseStreamObserver) {
        try {
            crossServerClient.cacheRefresh();
            responseStreamObserver.onNext(InvalidateServerCacheResponse.getDefaultInstance());
            responseStreamObserver.onCompleted();
        } catch (NoReplicasException e) {
            responseStreamObserver.onError(Status.CANCELLED.withDescription(NO_REPLICAS).asRuntimeException());
        }
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
        try {
            state.gossip(Timestamp.fromGrpc(request.getReplicaTS()), receivedOps);
    
            responseStreamObserver.onNext(PropagateStateResponse.newBuilder().build());
            responseStreamObserver.onCompleted();
        } catch (ServerUnavailableException e) {
            responseStreamObserver.onError(Status.UNAVAILABLE.withDescription(SERVER_UNAVAILABLE).asRuntimeException());
        }
    }
}
