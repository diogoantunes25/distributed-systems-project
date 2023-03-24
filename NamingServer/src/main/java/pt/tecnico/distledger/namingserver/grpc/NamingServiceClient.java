package pt.tecnico.distledger.namingserver.grpc;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import io.grpc.StatusRuntimeException;

import pt.ulisboa.tecnico.distledger.contract.distledgerserver.NamingServerDistLedger.*;
import pt.tecnico.distledger.namingserver.exceptions.ServerUnregistrationFailedException;
import pt.tecnico.distledger.namingserver.exceptions.ServerRegistrationFailedException;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.NamingServiceGrpc;

public class NamingServiceClient {
    private static final String NAMING_SERVER_TARGET = "localhost:5001";
    private final static int TIMEOUT = 100;
    
    final ManagedChannel channel;
    final NamingServiceGrpc.NamingServiceBlockingStub stub;

    public NamingServiceClient () {
        this.channel = ManagedChannelBuilder.forTarget(NAMING_SERVER_TARGET).usePlaintext().build();
        this.stub = NamingServiceGrpc.newBlockingStub(channel);
    }

    public void register(String serviceName, String serverQualifier, String serverAddress)
            throws ServerRegistrationFailedException {
        try {
            RegisterRequest request = RegisterRequest
                .newBuilder()
                .setServiceName(serviceName)
                .setQualifier(serverQualifier)
                .setAddress(serverAddress)
                .build();
    
            stub.withDeadlineAfter(TIMEOUT, TimeUnit.MILLISECONDS).register(request);
        } catch (StatusRuntimeException e) {
            throw new ServerRegistrationFailedException(serverAddress, serverQualifier, serviceName, e);
        }
    }

    public List<String> lookup(String serviceName, String serverQualifier) {
        LookupRequest request = LookupRequest
            .newBuilder()
            .setServiceName(serviceName)
            .setQualifier(serverQualifier)
            .build();
        LookupResponse response = stub.withDeadlineAfter(TIMEOUT, TimeUnit.MILLISECONDS).lookup(request);

        return response.getServicesList();
    }

    public void remove(String serviceName, String serverTarget) 
            throws ServerUnregistrationFailedException {
        try {
            RemoveRequest request = RemoveRequest
                .newBuilder()
                .setServiceName(serviceName)
                .setAddress(serverTarget)
                .build();
    
            stub.withDeadlineAfter(TIMEOUT, TimeUnit.MILLISECONDS).remove(request);
        } catch (StatusRuntimeException e) {
            throw new ServerUnregistrationFailedException(serverTarget, serviceName, e);
        }
    }

    public void delete() {
        this.channel.shutdown();
    }

}