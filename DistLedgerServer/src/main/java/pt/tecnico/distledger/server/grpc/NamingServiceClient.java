package pt.tecnico.distledger.server.grpc;

import java.util.List;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import io.grpc.StatusRuntimeException;

import pt.ulisboa.tecnico.distledger.contract.distledgerserver.NamingServerDistLedger.*;
import pt.tecnico.distledger.server.exceptions.ServerRegistrationFailedException;
import pt.tecnico.distledger.server.exceptions.ServerUnregistrationFailedException;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.NamingServiceGrpc;

public class NamingServiceClient {
    private static final String NAMING_SERVER_TARGET = "localhost:5001";
    final static ManagedChannel channel = ManagedChannelBuilder.forTarget(NAMING_SERVER_TARGET).usePlaintext().build();
    final static NamingServiceGrpc.NamingServiceBlockingStub stub = NamingServiceGrpc.newBlockingStub(channel);

    public static void register(String serviceName, String serverQualifier, String serverAddress)
            throws ServerRegistrationFailedException {
        try {
            RegisterRequest request = RegisterRequest
                .newBuilder()
                .setServiceName(serviceName)
                .setQualifier(serverQualifier)
                .setAddress(serverAddress)
                .build();
    
            stub.register(request);
        } catch (StatusRuntimeException e) {
            throw new ServerRegistrationFailedException(serverAddress, serverQualifier, serviceName, e);
        }
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
            throws ServerUnregistrationFailedException {
        try {
            RemoveRequest request = RemoveRequest
                .newBuilder()
                .setServiceName(serviceName)
                .setAddress(serverTarget)
                .build();
    
            stub.remove(request);
        } catch (StatusRuntimeException e) {
            throw new ServerUnregistrationFailedException(serverTarget, serviceName, e);
        }
    }

    public static void delete() {
        channel.shutdown();
    }

}