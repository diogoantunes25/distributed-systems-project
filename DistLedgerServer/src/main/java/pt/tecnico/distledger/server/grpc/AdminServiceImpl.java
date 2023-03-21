package pt.tecnico.distledger.server.grpc;

import java.util.List;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.DeleteOp;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.tecnico.distledger.server.exceptions.ServerRegistrationFailedException;
import pt.tecnico.distledger.server.exceptions.ServerUnregistrationFailedException;
import pt.tecnico.distledger.server.visitor.MessageConverterVisitor;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.LedgerState;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.LedgerStateOrBuilder;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.Operation;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.OperationType;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.NamingServerDistLedger;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.NamingServiceGrpc;

public class AdminServiceImpl extends AdminServiceGrpc.AdminServiceImplBase{

    private ServerState state;
    private final String qual;

    private final ManagedChannel nameServerChannel;
    private final NamingServiceGrpc.NamingServiceBlockingStub nameServerStub;
    private final static String SERVICE_NAME = "AdminService";

    public AdminServiceImpl(ServerState state, String qual, String nameServer) {
        this.state = state;
        this.qual = qual;

        this.nameServerChannel = ManagedChannelBuilder.forTarget(nameServer).usePlaintext().build();
        this.nameServerStub = NamingServiceGrpc.newBlockingStub(nameServerChannel);
    }

    public void register(String address) throws ServerRegistrationFailedException {
        try {
            NamingServerDistLedger.RegisterRequest request = NamingServerDistLedger.RegisterRequest
                    .newBuilder()
                    .setServiceName(SERVICE_NAME)
                    .setQualifier(qual)
                    .setAddress(address)
                    .build();

            nameServerStub.register(request);
        } catch (StatusRuntimeException e) {
            e.printStackTrace();
            throw new ServerRegistrationFailedException(address, qual, SERVICE_NAME, e);
        }
    }

    public void unregister(String address) throws ServerUnregistrationFailedException {
        try {
            NamingServerDistLedger.DeleteRequest request = NamingServerDistLedger.DeleteRequest
                    .newBuilder()
                    .setServiceName(SERVICE_NAME)
                    .setHostname(address)
                    .build();

            nameServerStub.delete(request);
        } catch (StatusRuntimeException e) {
            e.printStackTrace();
            throw new ServerUnregistrationFailedException(address, qual, SERVICE_NAME, e);
        }
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
