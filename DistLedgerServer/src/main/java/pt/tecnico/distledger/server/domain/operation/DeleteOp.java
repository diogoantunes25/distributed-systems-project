package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.gossip.Timestamp;
import pt.tecnico.distledger.server.domain.UpdateId;
import pt.tecnico.distledger.server.visitor.Visitor;

public class DeleteOp extends UpdateOp {

    public DeleteOp(Timestamp prev, Timestamp ts, UpdateId uid, String account) {
        super(prev, ts, uid, account);
    }

    @Override
    public UpdateOp getCopy() {
        return new DeleteOp(getPrev(), getTs(), getUid(), getAccount());
    }

    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
