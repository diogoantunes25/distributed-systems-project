package pt.tecnico.distledger.server.exceptions;

public class ServerRegistrationFailedException extends Exception{

    private static String server;
    private static String qual;
    private static String service;

    public ServerRegistrationFailedException(String server, String qual, String service, Throwable e) {
        super(String.format("Register for service %s at %s with qualifier %s failed", service, server, qual), e);
        this.server = server;
        this.qual = qual;
        this.service = service;
    }

    public static String getServer() {
        return server;
    }

    public static String getQual() {
        return qual;
    }

    public static String getService() {
        return service;
    }
}
