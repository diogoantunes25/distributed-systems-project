package pt.tecnico.distledger.adminclient.exceptions;

public class ServerUnavailableException extends Exception{

    public ServerUnavailableException(Throwable cause) {
        super("Server is not available", cause);
    }

}
