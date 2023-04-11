package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.gossip.Timestamp;
import pt.tecnico.distledger.server.domain.UpdateId;
import pt.tecnico.distledger.server.visitor.Visitor;

public class DeleteOp extends UpdateOp {

    public DeleteOp(Timestamp t, UpdateId uid, String account) {
        super(t, uid, account);
    }

    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
