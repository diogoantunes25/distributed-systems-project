package pt.tecnico.distledger.server.domain.exceptions;

public class AccountAlreadyExistsException extends DistLedgerException {
    private String userId;

    public AccountAlreadyExistsException(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}
