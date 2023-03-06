package pt.tecnico.distledger.server.domain.exceptions;

public class NotEnoughBalanceException extends DistLedgerException {
    private int availableBalance;
    private int desiredAmount;

    public NotEnoughBalanceException(int availableBalance, int desiredAmount) {
        this.availableBalance = availableBalance;
        this.desiredAmount = desiredAmount;
    }

    public int getAvailableBalance() {
        return availableBalance;
    }

    public int getDesiredAmount() {
        return desiredAmount;
    }
}