package pt.tecnico.distledger.adminclient.exceptions;

public class ServerLookupFailedException extends Exception {
    private String server;

    public ServerLookupFailedException(String server) {
        this(server, null);
    }

    public ServerLookupFailedException(String server, Throwable cause) {
        super(String.format("Lookup for %s failed", server), cause);
        this.server = server;
    }
}
