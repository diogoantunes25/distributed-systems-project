package pt.tecnico.distledger.server.grpc;

import io.grpc.stub.StreamObserver;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.visitor.MessageConverterVisitor;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.LedgerState;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;

public class AdminServiceImpl extends AdminServiceGrpc.AdminServiceImplBase{

    private ServerState state;

    public AdminServiceImpl(ServerState state) {
        this.state = state;
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
        // state.gossip();
        GossipResponse response = GossipResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getLedgerState(getLedgerStateRequest request, StreamObserver<getLedgerStateResponse> responseObserver) {
        // TODO: is this synchronized?
        LedgerState.Builder ledger = LedgerState.newBuilder();
        state.getLedgerState().stream()
              .map(o -> o.accept(new MessageConverterVisitor()))
              .forEach(v -> ledger.addLedger(v));

        getLedgerStateResponse response = getLedgerStateResponse.newBuilder().setLedgerState(ledger).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
