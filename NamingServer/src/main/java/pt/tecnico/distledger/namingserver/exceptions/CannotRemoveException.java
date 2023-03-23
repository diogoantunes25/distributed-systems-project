package pt.tecnico.distledger.namingserver.exceptions;

public class CannotRemoveException extends NameServerException {
    private String serviceName;
    private String hostname;
    private int port;

    public CannotRemoveException(String serviceName, String hostname, int port, Throwable cause) {
        super(String.format("Cannot remove service %s at %s:%s", serviceName ,hostname, port), cause);
        this.serviceName = serviceName;
        this.hostname = hostname;
        this.port = port;
    }

    public CannotRemoveException(String serviceName, String hostname, int port) {
        this.serviceName = serviceName;
        this.hostname = hostname;
        this.port = port;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}

