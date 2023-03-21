package pt.tecnico.distledger.namingserver;

import pt.tecnico.distledger.namingserver.exceptions.CannotDeleteException;
import pt.tecnico.distledger.namingserver.exceptions.NoSuchServerException;

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

    public Set<ServerEntry> getServers() {
        return servers;
    }

    public List<ServerEntry> getServers(final String qualifier) {
        return servers.stream().filter(se -> se.getQualifier().compareTo(qualifier) == 0).collect(Collectors.toList());
    }

    public void setServers(Set<ServerEntry> servers) {
        this.servers = servers;
    }

    public void addServer(ServerEntry server) {
        this.servers.add(server);
    }

    public void deleteServer(String hostname, int port)
        throws NoSuchServerException {
        for (ServerEntry entry: servers) {
            if (entry.getHostname().compareTo(hostname) == 0 && entry.getPort() == port) {
                servers.remove(entry);
                return;
            }
        }

        throw new NoSuchServerException(hostname, port);
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
