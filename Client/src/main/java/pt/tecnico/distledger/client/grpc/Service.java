package pt.tecnico.distledger.client.grpc;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import io.grpc.Server;
import pt.tecnico.distledger.client.exceptions.ServerLookupFailedException;
import pt.tecnico.distledger.client.exceptions.ServerUnavailableException;
import pt.tecnico.distledger.namingserver.grpc.NamingServiceClient;
import pt.tecnico.distledger.namingserver.NamingServer;

public abstract class Service {
    protected final static int SHORT_TIMEOUT = 5000; // 5 milliseconds
    protected final static int LONG_TIMEOUT = 5000 * 60; // 5 minutes
    
    // Caches ManagedChannel for qualifier
    protected final Map<String, ManagedChannel> serverCache = new HashMap<>();
    protected final NamingServiceClient namingServiceClient = new NamingServiceClient();
    protected final long id = fetchId();

    public long getId() {
        return this.id;
    }

    protected ManagedChannel getServerChannel(String server) 
            throws ServerUnavailableException {
        if (this.serverCache.containsKey(server)) return this.serverCache.get(server);
        throw new ServerUnavailableException();
    }

    private boolean cacheHasServerEntry(String server) {
        return this.serverCache.containsKey(server);
    }
    
    private long fetchId() {
        return this.namingServiceClient.getClientId();
    }

    protected void cacheRefresh(String qualifier) throws ServerLookupFailedException {
        System.out.printf("[Service] refreshing cache\n");
        List<String> servers = this.namingServiceClient
            .lookup(NamingServer.SERVICE_NAME, qualifier);

        if(servers.isEmpty()) {
            throw new ServerLookupFailedException();
        }

        System.out.printf("[Service] New server %s is %s\n", qualifier, servers.get(0));

        ManagedChannel channel = ManagedChannelBuilder
            .forTarget(servers.get(0))
            .usePlaintext()
            .build();

        this.serverCache.put(qualifier, channel);
    }

    protected void removeServer(String server) {
        try {
            this.getServerChannel(server).shutdown();
        } catch (ServerUnavailableException e) {
            System.out.printf("[Service] remove server failed\n");
        }
        this.serverCache.remove(server);
    }

    public void delete() {
        for (String server : this.serverCache.keySet()) removeServer(server);
        this.namingServiceClient.delete();
    }

}
