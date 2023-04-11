package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.gossip.Timestamp;
import pt.tecnico.distledger.server.visitor.Visitor;

public class GetBalanceOp extends ReadOp {
    String userId;

    public GetBalanceOp(Timestamp t, String userId) {
        super(t);
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public <T> T accept(Visitor<T> visitor) {
        return (T) visitor.visit(this);
    }
}
