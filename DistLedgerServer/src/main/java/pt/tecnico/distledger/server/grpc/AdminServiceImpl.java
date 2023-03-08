package pt.tecnico.distledger.server.grpc;

import java.util.List;

import io.grpc.stub.StreamObserver;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.DeleteOp;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.LedgerState;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.LedgerStateOrBuilder;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.Operation;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.OperationType;
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

        LedgerState.Builder ledger = LedgerState.newBuilder();
        state.getLedgerState().stream()
              .map(o -> convertToMessage(o))
              .forEach(v -> ledger.addLedger(v));
        
        getLedgerStateResponse response = getLedgerStateResponse.newBuilder().setLedgerState(ledger).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    public static Operation convertToMessage(CreateOp op) {
        return Operation.newBuilder()
                        .setType(OperationType.OP_CREATE_ACCOUNT)
                        .setUserId(op.getAccount())
                        .build();
    }

    public static Operation convertToMessage(DeleteOp op) {
        return Operation.newBuilder()
                        .setType(OperationType.OP_DELETE_ACCOUNT)
                        .setUserId(op.getAccount())
                        .build();
    }

    public static Operation convertToMessage(TransferOp op) {
        return Operation.newBuilder()
                        .setType(OperationType.OP_TRANSFER_TO)
                        .setUserId(op.getAccount())
                        .setDestUserId(op.getDestAccount())
                        .setAmount(op.getAmount())
                        .build();
    }

    public static Operation convertToMessage(pt.tecnico.distledger.server.domain.operation.Operation op) {
        if (op instanceof CreateOp) {
            return convertToMessage((CreateOp) op);
        } else if (op instanceof  DeleteOp) {
            return convertToMessage((DeleteOp) op);
        } else if (op instanceof TransferOp) {
            return convertToMessage((TransferOp) op);
        }

        throw new UnsupportedOperationException();
    }
}
