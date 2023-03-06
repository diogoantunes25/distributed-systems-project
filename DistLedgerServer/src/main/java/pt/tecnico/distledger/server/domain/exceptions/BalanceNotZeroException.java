package pt.tecnico.distledger.server.domain.exceptions;

public class BalanceNotZeroException extends DistLedgerException {
    private String userId;
    private int balance;

    public BalanceNotZeroException(String userId, int balance) {
        this.userId = userId;
        this.balance = balance;
    }

    public String getUserId() {
        return userId;
    }

    public int getBalance() {
        return balance;
    }
}
