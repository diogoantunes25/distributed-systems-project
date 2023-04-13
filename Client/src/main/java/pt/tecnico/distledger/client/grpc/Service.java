package pt.tecnico.distledger.client.grpc;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import pt.tecnico.distledger.client.exceptions.ServerLookupFailedException;
import pt.tecnico.distledger.client.exceptions.ServerUnavailableException;
import pt.tecnico.distledger.namingserver.grpc.NamingServiceClient;
import pt.tecnico.distledger.namingserver.NamingServer;

public abstract class Service {
    protected final static int TIMEOUT = 5000; // milliseconds
    
    // Caches ManagedChannel for qualifier
    protected final Map<String, ManagedChannel> serverCache = new HashMap<>();
    protected final NamingServiceClient namingServiceClient = new NamingServiceClient();
    protected final long id = fetchId();

    public long getId() {
        return this.id;
    }

    protected ManagedChannel getServerChannel(String server) 
            throws ServerUnavailableException {
        return this.serverCache.get(server);
    }
    
    private boolean cacheHasServerEntry(String server) {
        return this.serverCache.containsKey(server);
    }
    
    private long fetchId() {
        return this.namingServiceClient.getClientId();
    }

    protected void cacheRefresh() throws ServerLookupFailedException {
        List<String> servers = this.namingServiceClient
            .lookup(NamingServer.SERVICE_NAME, "");
        if(servers.isEmpty()) {
            throw new ServerLookupFailedException();
        }

        for (String server : servers) {
            if (!cacheHasServerEntry(server)) {
                ManagedChannel channel = ManagedChannelBuilder
                    .forTarget(server)
                    .usePlaintext()
                    .build();
                this.serverCache.put(server, channel);
            }
        }
    }

    protected void removeServer(String server) {
        try {
            this.getServerChannel(server).shutdown();
        } catch (ServerUnavailableException e) { }
        this.serverCache.remove(server);
    }

    public void delete() {
        for (String server : this.serverCache.keySet()) removeServer(server);
        this.namingServiceClient.delete();
    }

}
