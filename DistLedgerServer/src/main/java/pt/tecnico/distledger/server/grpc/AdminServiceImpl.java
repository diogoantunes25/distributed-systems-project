package pt.tecnico.distledger.server.grpc;

import java.util.concurrent.locks.ReentrantLock;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import pt.tecnico.distledger.gossip.Timestamp;
import pt.tecnico.distledger.server.exceptions.CannotGossipException;
import pt.tecnico.distledger.server.exceptions.NoReplicasException;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.LedgerState;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.visitor.MessageConverterVisitor;

public class AdminServiceImpl extends AdminServiceGrpc.AdminServiceImplBase{

    private ServerState state;
    private CrossServerClient crossServerService;

    private final static String CANNOT_GOSSIP = "Failed to find replica to gossip with";

    public AdminServiceImpl(ServerState state, String qual) {
        this.state = state;
        this.crossServerService = new CrossServerClient(state, qual);
    }

    @Override
    public void activate(ActivateRequest request, StreamObserver<ActivateResponse> responseObserver) {
        state.activate();

        ActivateResponse response = ActivateResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void deactivate(DeactivateRequest request, StreamObserver<DeactivateResponse> responseObserver) {
        state.deactivate();

        DeactivateResponse response = DeactivateResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void gossip(GossipRequest request, StreamObserver<GossipResponse> responseObserver) {
        System.out.printf("[AdminServiceImpl] gossip requested\n");

        try {
            crossServerService.propagateState();
            GossipResponse response = GossipResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (CannotGossipException | NoReplicasException e) {
            e.printStackTrace();
            responseObserver.onError(Status.UNAVAILABLE.withDescription(CANNOT_GOSSIP).asRuntimeException());
        }
    }

    @Override
    public void getLedgerState(getLedgerStateRequest request, StreamObserver<getLedgerStateResponse> responseObserver) {
        // TODO
    }
}
