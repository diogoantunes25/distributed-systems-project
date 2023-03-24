package pt.tecnico.distledger.namingserver.exceptions;

public class ServerRegistrationFailedException extends Exception{

    private String server;
    private String qual;
    private String service;

    public ServerRegistrationFailedException(String server, String qual, String service, Throwable e) {
        super(String.format("Register for service %s at %s with qualifier %s failed", service, server, qual), e);
        this.server = server;
        this.qual = qual;
        this.service = service;
    }

    public String getServer() {
        return server;
    }

    public String getQual() {
        return qual;
    }

    public String getService() {
        return service;
    }
}
