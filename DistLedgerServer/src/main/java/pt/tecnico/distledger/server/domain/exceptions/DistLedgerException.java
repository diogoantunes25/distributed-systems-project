package pt.tecnico.distledger.server.domain.exceptions;

public class DistLedgerException extends Exception {

    public DistLedgerException(String message, Throwable cause) {
        super(message, cause);
    }

    public DistLedgerException() {}
}