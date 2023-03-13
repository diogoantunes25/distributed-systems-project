package pt.tecnico.distledger.namingserver.grpc;

import pt.tecnico.distledger.namingserver.exceptions.DuplicateServiceException;
import pt.tecnico.distledger.namingserver.NamingServer;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.NamingServerDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.NamingServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

public class NamingServerServiceImpl extends NamingServiceGrpc.NamingServiceImplBase {

    private NamingServer server;
    public NamingServerServiceImpl(NamingServer server) {
        this.server = server;
    }

    private final static String DUPLICATE_SERVICE = "Service at this host already exists";

    @Override
    public void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {
        try {
            String hostname = request.getAddress().split(":", 0)[0];
            int port = Integer.parseInt(request.getAddress().split(":", 0)[0]);
            server.register(request.getServiceName(), request.getQualifier(), hostname, port);
            responseObserver.onNext(RegisterResponse.newBuilder().build());
            responseObserver.onCompleted();
        } catch (DuplicateServiceException e) {
            responseObserver.onError(Status.ALREADY_EXISTS.withDescription(DUPLICATE_SERVICE).asRuntimeException());
        }
    }
}
