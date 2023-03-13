package pt.tecnico.distledger.namingserver.exceptions;

public class DuplicateServiceException extends NameServerException {

    private String serviceName;
    private String hostname;
    private int port;

    public DuplicateServiceException(String serviceName, String hostname, int port) {
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
