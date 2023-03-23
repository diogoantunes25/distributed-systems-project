package pt.tecnico.distledger.server.grpc;

import java.util.List;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import io.grpc.StatusRuntimeException;

import pt.ulisboa.tecnico.distledger.contract.distledgerserver.NamingServerDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.NamingServiceGrpc;

public class NamingService {
    private static final String NAMING_SERVER_TARGET = "localhost:5001";
    final static ManagedChannel channel = ManagedChannelBuilder.forTarget(NAMING_SERVER_TARGET).usePlaintext().build();
    final static NamingServiceGrpc.NamingServiceBlockingStub stub = NamingServiceGrpc.newBlockingStub(channel);

    public static void register(String serviceName, String serverQualifier, String serverAddress)
            throws StatusRuntimeException {
        // TODO: should we just pass on this exception?
        RegisterRequest request = RegisterRequest
            .newBuilder()
            .setServiceName(serviceName)
            .setQualifier(serverQualifier)
            .setAddress(serverAddress)
            .build();

        stub.register(request);
    }

    public static List<String> lookup(String serviceName, String serverQualifier) {
        LookupRequest request = LookupRequest
            .newBuilder()
            .setServiceName(serviceName)
            .setQualifier(serverQualifier)
            .build();
        LookupResponse response = stub.lookup(request);

        return response.getServicesList();
    }

    public static void remove(String serviceName, String serverTarget) 
            throws StatusRuntimeException {
        // TODO: should we just pass on this exception?
        RemoveRequest request = RemoveRequest
            .newBuilder()
            .setServiceName(serviceName)
            .setAddress(serverTarget)
            .build();

        stub.remove(request);
    }

    public static void delete() {
        channel.shutdown();
    }

}