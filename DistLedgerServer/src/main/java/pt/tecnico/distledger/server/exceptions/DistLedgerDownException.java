package pt.tecnico.distledger.server.exceptions;

public class DistLedgerDownException extends Exception {
    public DistLedgerDownException() {}

    public DistLedgerDownException(Throwable cause) {
        super("DistLedger is currently unavailable", cause);
    }
}
