package pt.tecnico.distledger.client.exceptions;

public class ServerUnavailableException extends Exception {

    public ServerUnavailableException(Throwable cause) {
        super("Server is not available", cause);
    }

}
