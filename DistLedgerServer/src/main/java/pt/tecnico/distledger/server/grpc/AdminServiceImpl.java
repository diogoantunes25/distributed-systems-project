package pt.tecnico.distledger.server.grpc;

import java.util.List;

import io.grpc.stub.StreamObserver;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.DeleteOp;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.tecnico.distledger.server.visitor.MessageConverterVisitor;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.LedgerState;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.LedgerStateOrBuilder;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.Operation;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.OperationType;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;

public class AdminServiceImpl extends AdminServiceGrpc.AdminServiceImplBase{

    private ServerState state;
    private final String qual;

    public AdminServiceImpl(ServerState state, String qual) {
        this.state = state;
        this.qual = qual;
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
        LedgerState.Builder ledger = LedgerState.newBuilder();
        state.getLedgerState().stream()
              .map(o -> o.accept(new MessageConverterVisitor()))
              .forEach(v -> ledger.addLedger(v));

        getLedgerStateResponse response = getLedgerStateResponse.newBuilder().setLedgerState(ledger).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
