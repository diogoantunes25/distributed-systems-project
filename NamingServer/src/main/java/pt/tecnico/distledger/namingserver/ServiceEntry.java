package pt.tecnico.distledger.namingserver;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    public List<ServerEntry> getServers() {
        return servers.stream().collect(Collectors.toList());
    }

    public List<ServerEntry> getServers(final String qualifier) {
        return servers.stream().filter(se -> se.getQualifier().compareTo(qualifier) == 0).collect(Collectors.toList());
    }

    public ServerEntry getServer(String hostName, int port) {
        for (ServerEntry server : servers) {
            if (server.getHostname().equals(hostName) && server.getPort() == port) {
                return server;
            }
        }
        return null;
    }

    public void setServers(Set<ServerEntry> servers) {
        this.servers = servers;
    }

    public void addServer(ServerEntry server) {
        this.servers.add(server);
    }

    public void removeServer(ServerEntry server) {
        servers.remove(server);
    }

    public boolean hasServer(String hostname, int port) {
        for (ServerEntry server: servers) {
            if (server.getHostname().compareTo(hostname) == 0 && server.getPort() == port) {
                return true;
            }
        }

        return false;
    }

    public boolean hasServers() {
        return !servers.isEmpty();
    }
}
