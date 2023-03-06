package pt.tecnico.distledger.server.domain.exceptions;

public class AccountDoesNotExistException extends DistLedgerException {
    private String userId;

    public AccountDoesNotExistException(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}
