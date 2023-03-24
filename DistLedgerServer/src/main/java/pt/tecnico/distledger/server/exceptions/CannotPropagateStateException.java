package pt.tecnico.distledger.server.exceptions;

public class CannotPropagateStateException extends Exception {

    private String server;

    public CannotPropagateStateException(String server, Throwable e) {
        super(String.format("Cannot propagate state to server %s", server), e);
    }

    public String getServer() {
        return server;
    }
 }
    
