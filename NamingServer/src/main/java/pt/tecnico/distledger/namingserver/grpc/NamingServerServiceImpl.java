package pt.tecnico.distledger.namingserver.grpc;

import pt.tecnico.distledger.namingserver.ServerEntry;
import pt.tecnico.distledger.namingserver.exceptions.CannotDeleteException;
import pt.tecnico.distledger.namingserver.exceptions.DuplicateServiceException;
import pt.tecnico.distledger.namingserver.NamingServer;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.NamingServerDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.NamingServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.List;
import java.util.stream.Collectors;

public class NamingServerServiceImpl extends NamingServiceGrpc.NamingServiceImplBase {

    private NamingServer server;
    public NamingServerServiceImpl(NamingServer server) {
        this.server = server;
    }

    private final static String DUPLICATE_SERVICE = "Service at this host already exists";
    private final static String CANNOT_REMOVE_SERVER = "Not possible to remove the server";


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

    @Override
    public void lookup(LookupRequest request, StreamObserver<LookupResponse> responseObserver) {
        List<ServerEntry> servers = server.lookup(request.getServicename(), request.getQualifier());
        LookupResponse response = LookupResponse.newBuilder()
                .addAllServices(servers.stream()
                                            .map(se -> String.format("%s:%s", se.getHostname(), se.getPort()))
                                            .collect(Collectors.toList()))
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void delete(DeleteRequest request, StreamObserver<DeleteResponse> responseObserver) {
        try {
            server.delete(request.getServiceName(), request.getHostname(), request.getPort());
            responseObserver.onNext(DeleteResponse.newBuilder().build());
        } catch (CannotDeleteException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(CANNOT_REMOVE_SERVER).asRuntimeException());
        }
    }
}
