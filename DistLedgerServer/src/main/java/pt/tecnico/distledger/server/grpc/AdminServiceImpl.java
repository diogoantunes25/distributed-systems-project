package pt.tecnico.distledger.server.grpc;

import java.util.concurrent.locks.ReentrantLock;

import io.grpc.stub.StreamObserver;

import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.LedgerState;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.visitor.MessageConverterVisitor;

public class AdminServiceImpl extends AdminServiceGrpc.AdminServiceImplBase{

    private ServerState state;
    private ReentrantLock lock;

    public AdminServiceImpl(ServerState state, ReentrantLock lock) {
        this.state = state;
        this.lock = lock;
    }

    @Override
    public void activate(ActivateRequest request, StreamObserver<ActivateResponse> responseObserver) {
        System.out.println(request);
        try {
            lock.lock();
            state.activate();
        } finally {
            lock.unlock();
        }

        ActivateResponse response = ActivateResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void deactivate(DeactivateRequest request, StreamObserver<DeactivateResponse> responseObserver) {
        System.out.println(request);
        try {
            lock.lock();
            state.deactivate();
        } finally {
            lock.unlock();
        }

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
        System.out.println(request);
        MessageConverterVisitor visitor = new MessageConverterVisitor();

        LedgerState.Builder ledgerStateBuilder = LedgerState.newBuilder();
        state.getLedgerState().stream()
              .forEach(o -> ledgerStateBuilder.addLedger(o.accept(visitor)));

        getLedgerStateResponse response = getLedgerStateResponse.newBuilder()
                .setLedgerState(ledgerStateBuilder.build())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
