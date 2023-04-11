package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.gossip.Timestamp;
import pt.tecnico.distledger.server.domain.UpdateId;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class UpdateOp extends Operation {
    private String account;
    private UpdateId uid;
    private AtomicBoolean stable;

    public UpdateOp(Timestamp t, UpdateId uid, String fromAccount) {
        super(t);
        this.account = fromAccount;
        this.uid = uid;
        this.stable = new AtomicBoolean();
    }

    public String getAccount() {
        return account;
    }

    public void setStable() {
        this.stable.set(true);
    }

    public boolean isStable() {
        return stable.get();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UpdateOp updateOp = (UpdateOp) o;
        return uid.equals(updateOp.uid);
    }

    @Override
    public int hashCode() {
        return uid.hashCode();
    }
}
