package pt.tecnico.distledger.server.exceptions;

public class CannotGossipException extends Exception {

    private String server;

    public CannotGossipException() {
        super();
    }

    public CannotGossipException(String server, Throwable e) {
        super(String.format("Cannot propagate state to server %s", server), e);
    }

    public String getServer() {
        return server;
    }
 }
    
