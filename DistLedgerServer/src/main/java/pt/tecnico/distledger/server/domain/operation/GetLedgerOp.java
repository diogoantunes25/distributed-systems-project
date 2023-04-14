package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.gossip.Timestamp;
import pt.tecnico.distledger.server.visitor.Visitor;

public class GetLedgerOp extends ReadOp {
    public GetLedgerOp(Timestamp t) {
        super(t);
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
