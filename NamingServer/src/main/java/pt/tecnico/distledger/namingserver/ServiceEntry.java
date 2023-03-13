package pt.tecnico.distledger.namingserver;

import java.util.HashSet;
import java.util.Set;

public class ServiceEntry {

    Set<ServerEntry> servers;
    String serviceName;

    public ServiceEntry(String serviceName) {
        this(serviceName, new HashSet<ServerEntry>());
    }

    public ServiceEntry(String serviceName, Set<ServerEntry> servers) {
        this.servers = servers;
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Set<ServerEntry> getServers() {
        return servers;
    }

    public void setServers(Set<ServerEntry> servers) {
        this.servers = servers;
    }

    public void addServer(ServerEntry server) {
        this.servers.add(server);
    }

    public boolean hasServer(String hostname, int port) {
        for (ServerEntry server: servers) {
            if (server.getHostname().compareTo(hostname) == 0 && server.getPort() == port) {
                return true;
            }
        }

        return false;
    }
}
