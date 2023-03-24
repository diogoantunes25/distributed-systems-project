package pt.tecnico.distledger.server.exceptions;

public class ServerUnregistrationFailedException extends Exception {

    private static String server;
    private static String service;

    public ServerUnregistrationFailedException(String server, String service, Throwable e) {
        super(String.format("Register for service %s at %s with qualifier %s failed", service, server), e);
        this.server = server;
        this.service = service;
    }

    public static String getServer() {
        return server;
    }

    public static String getService() {
        return service;
    }
}
