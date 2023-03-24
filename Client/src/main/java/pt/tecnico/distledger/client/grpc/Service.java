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
    protected NamingServiceClient namingServiceClient = new NamingServiceClient();
    protected final static int TIMEOUT = 100; // milliseconds

    // Caches ManagedChannel for qualifier
    protected final Map<String, ManagedChannel> serverCache = new HashMap<>();

    protected boolean cacheHasServerEntry(String server) {
        return serverCache.containsKey(server);
    }

    protected void cacheUpdate(String server, ManagedChannel channel) {
        serverCache.put(server, channel);
    }

    protected void cacheRefresh(String qual) throws ServerLookupFailedException {
        List<String> servers = this.namingServiceClient.lookup(NamingServer.SERVICE_NAME, qual);
        if(servers.isEmpty()) {
            throw new ServerLookupFailedException(qual);
        }

        if (servers.size() > 1){
            // We assume there is only one secondary server active at every moment
            System.out.println("WARNING: More than one secondary server found");
        }

        cacheUpdate(qual, ManagedChannelBuilder.forTarget(servers.get(0)).usePlaintext().build());
    }

    protected ManagedChannel getServerChannel(String server) {
        return serverCache.get(server);
    }

    public void delete() {
        namingServiceClient.delete();
    }

}
