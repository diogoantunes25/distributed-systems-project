package pt.tecnico.distledger.client.grpc;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import pt.tecnico.distledger.client.exceptions.ServerLookupFailedException;
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
    
    protected long fetchId() {
        return this.namingServiceClient.getClientId();
    }
    
    protected boolean cacheHasServerEntry(String server) {
        return this.serverCache.containsKey(server);
    }

    protected void cacheUpdate(String server, ManagedChannel channel) {
        this.serverCache.put(server, channel);
    }

    protected void cacheRefresh(String qual) throws ServerLookupFailedException {
        List<String> servers = this.namingServiceClient.lookup(NamingServer.SERVICE_NAME, qual);
        if(servers.isEmpty()) {
            throw new ServerLookupFailedException(qual);
        }

        cacheUpdate(qual, ManagedChannelBuilder.forTarget(servers.get(0)).usePlaintext().build());
    }

    protected ManagedChannel getServerChannel(String server) {
        return this.serverCache.get(server);
    }

    protected void removeServer(String server) {
        this.getServerChannel(server).shutdown();
        this.serverCache.remove(server);
    }

    public void delete() {
        for (String server : this.serverCache.keySet()) removeServer(server);
        this.namingServiceClient.delete();
    }

}
