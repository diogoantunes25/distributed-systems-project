package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.gossip.Timestamp;
import pt.tecnico.distledger.server.domain.UpdateId;
import pt.tecnico.distledger.server.visitor.Visitor;

public class TransferOp extends UpdateOp {
    private String destAccount;
    private int amount;

    public TransferOp(Timestamp prev, Timestamp ts, UpdateId uid, String fromAccount, String destAccount, int amount) {
        super(prev, uid, fromAccount);
        this.destAccount = destAccount;
        this.amount = amount;
        setTs(ts);
    }
    public TransferOp(Timestamp prev, UpdateId uid, String fromAccount, String destAccount, int amount) {
        this(prev, null, uid, fromAccount, destAccount, amount);
    }

    public String getDestAccount() {
        return destAccount;
    }

    public void setDestAccount(String destAccount) {
        this.destAccount = destAccount;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public UpdateOp getCopy() {
        return new TransferOp(getPrev(), getTs(), getUid(), getAccount(), getDestAccount(), getAmount());
    }
}
