package pt.tecnico.distledger.server.grpc;

import java.util.List;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import io.grpc.StatusRuntimeException;
import pt.tecnico.distledger.server.exceptions.CannotGetServerAddressesException;

import pt.ulisboa.tecnico.distledger.contract.distledgerserver.NamingServerDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.NamingServiceGrpc;


import pt.tecnico.distledger.server.domain.exceptions.*;

public class NamingServerServiceImpl {
    private static final String NAMING_SERVER_TARGET = "localhost:5001";
    final static ManagedChannel channel = ManagedChannelBuilder.forTarget(NAMING_SERVER_TARGET).usePlaintext().build();
    final static NamingServiceGrpc.NamingServiceBlockingStub stub = NamingServiceGrpc.newBlockingStub(channel);

    public static void registerServer(String serviceName, String serverQualifier, String serverAddress)
            throws StatusRuntimeException {
        // TODO: handle exceptions (compare with previous version): throw ServerRegistrationFailedException?
        RegisterRequest request = RegisterRequest
            .newBuilder()
            .setServiceName(serviceName)
            .setQualifier(serverQualifier)
            .setAddress(serverAddress)
            .build();

        stub.register(request);
    }

    public static List<String> getServerAddresses(String serviceName, String serverQualifier) 
            throws CannotGetServerAddressesException {
        try {
            LookupRequest request = LookupRequest
                .newBuilder()
                .setServicename(serviceName)
                .setQualifier(serverQualifier)
                .build();
            LookupResponse response = stub.lookup(request);

            return response.getServicesList();
        } catch (StatusRuntimeException e) {
            throw new CannotGetServerAddressesException(serviceName, serverQualifier);
        }
    }

    public static void unregisterServer(String serviceName, String serverTarget) 
            throws StatusRuntimeException {
        // TODO: handle exceptions (compare with previous version): throw ServerUnregistrationFailedException?
        DeleteRequest request = DeleteRequest
            .newBuilder()
            .setServiceName(serviceName)
            .setHostname(serverTarget)
            .build();

        stub.delete(request);
    }

    public static void delete() {
        channel.shutdown();
    }

}