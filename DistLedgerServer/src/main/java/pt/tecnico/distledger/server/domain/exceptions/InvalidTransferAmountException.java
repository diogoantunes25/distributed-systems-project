package pt.tecnico.distledger.server.domain.exceptions;

public class InvalidTransferAmountException extends DistLedgerException {
    private int amount;

    public InvalidTransferAmountException(int amount) {
        this.amount = amount;
    }

    public int getAmount() {
        return amount;
    }
}
