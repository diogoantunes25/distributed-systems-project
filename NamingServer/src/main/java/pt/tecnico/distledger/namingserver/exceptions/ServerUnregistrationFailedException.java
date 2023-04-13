package pt.tecnico.distledger.namingserver.exceptions;

public class ServerUnregistrationFailedException extends Exception {

    private String server;
    private String service;

    public ServerUnregistrationFailedException(String server, String service, Throwable e) {
        super(String.format("Unregister for service %s at %s failed", service, server), e);
        this.server = server;
        this.service = service;
    }

    public String getServer() {
        return server;
    }

    public String getService() {
        return service;
    }
}
