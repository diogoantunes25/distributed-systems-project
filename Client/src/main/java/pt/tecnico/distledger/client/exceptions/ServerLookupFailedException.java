package pt.tecnico.distledger.client.exceptions;

public class ServerLookupFailedException extends Exception {
    private String server;

    public ServerLookupFailedException() {
        super(String.format("Lookup failed"));
    }

    public ServerLookupFailedException(Throwable cause) {
        super(String.format("Lookup failed"), cause);
    }

    public ServerLookupFailedException(String server) {
        super(String.format("Lookup for %s failed", server), null);
        this.server = server;
    }

    public ServerLookupFailedException(String server, Throwable cause) {
        super(String.format("Lookup for %s failed", server), cause);
        this.server = server;
    }

    public String getServer() {
        return server;
    }
}
