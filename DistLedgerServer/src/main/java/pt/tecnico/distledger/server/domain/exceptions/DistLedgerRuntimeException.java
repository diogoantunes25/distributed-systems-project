package pt.tecnico.distledger.server.domain.exceptions;

public class DistLedgerRuntimeException extends RuntimeException {
    
    public DistLedgerRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public DistLedgerRuntimeException() {}
}
