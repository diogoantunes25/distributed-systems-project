package pt.tecnico.distledger.namingserver.grpc;

import java.util.List;
import java.util.stream.Collectors;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import pt.tecnico.distledger.namingserver.ServerEntry;
import pt.tecnico.distledger.namingserver.NamingServer;
import pt.tecnico.distledger.namingserver.exceptions.CannotRemoveException;
import pt.tecnico.distledger.namingserver.exceptions.CannotRegisterException;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc.*;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.NamingServerDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.NamingServiceGrpc;

public class NamingServerServiceImpl extends NamingServiceGrpc.NamingServiceImplBase {

    private NamingServer namingServer;
    public NamingServerServiceImpl(NamingServer namingServer) {
        this.namingServer = namingServer;
    }

    private final String NOT_POSSIBLE_TO_REGISTER = "Not Possible To Register The Server";
    private final String NOT_POSSIBLE_TO_REMOVE = "Not Possible To Remove The Server";

    @Override
    public void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {
        try {
            String serviceName = request.getServiceName();
            String serverQualifier = request.getQualifier();
            String serverAddress = request.getAddress();
            String hostName = serverAddress.split(":")[0];
            int port = Integer.parseInt(serverAddress.split(":")[1]);

            // Invalidate cache of all servers that provide serviceName
            for (ServerEntry entry: namingServer.lookup(serviceName, "")) {
                // Invalidation is done in background. Error is not relevant
                new Thread(() -> {
                    ManagedChannel channel = ManagedChannelBuilder.forAddress(entry.getHostname(), entry.getPort()).usePlaintext().build();
                    try {
                        DistLedgerCrossServerServiceBlockingStub stub = DistLedgerCrossServerServiceGrpc.newBlockingStub(channel);
                        stub.invalidateCache(CrossServerDistLedger.InvalidateServerCacheRequest.getDefaultInstance());
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.printf("Invalidate cache of %s:%s failed\n", entry.getHostname(), entry.getPort());
                    } finally {
                        channel.shutdown();
                    }
                }).start();
            }

            namingServer.register(serviceName, serverQualifier, hostName, port);

            responseObserver.onNext(RegisterResponse.newBuilder().build());
            responseObserver.onCompleted();
        } catch (CannotRegisterException e) {
            responseObserver.onError(Status.ALREADY_EXISTS
                .withDescription(NOT_POSSIBLE_TO_REGISTER)
                .asRuntimeException());
        }
    }

    @Override
    public void lookup(LookupRequest request, StreamObserver<LookupResponse> responseObserver) {
        System.out.printf("[NamingServerServiceImpl] lookup request received\n");
        List<ServerEntry> servers = namingServer
            .lookup(request.getServiceName(), request.getQualifier());

        LookupResponse response = LookupResponse.newBuilder()
                .addAllServices(
                    servers.stream()
                        .map(se -> String.format("%s:%s", se.getHostname(), se.getPort()))
                        .collect(Collectors.toList())
                ).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void remove(RemoveRequest request, StreamObserver<RemoveResponse> responseObserver) {
        try {
            String serviceName = request.getServiceName();
            String serverQualifier = request.getAddress();
            String hostName = serverQualifier.split(":")[0];
            int port = Integer.parseInt(serverQualifier.split(":")[1]);

            this.namingServer.remove(serviceName, hostName, port);
            responseObserver.onNext(RemoveResponse.newBuilder().build());
            responseObserver.onCompleted();
        } catch (CannotRemoveException e) {
            responseObserver.onError(Status.NOT_FOUND
                .withDescription(NOT_POSSIBLE_TO_REMOVE)
                .asRuntimeException());
        }
    }

    @Override
    public void getClientId(GetClientIdRequest request, StreamObserver<GetClientIdResponse> responseObserver) {
        responseObserver.onNext(GetClientIdResponse.newBuilder().setClientId(
            namingServer.getClientId()
        ).build());
        responseObserver.onCompleted();
    }
}
