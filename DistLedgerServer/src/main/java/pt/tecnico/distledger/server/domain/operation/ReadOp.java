package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.gossip.Timestamp;
import pt.tecnico.distledger.server.visitor.Visitor;

public abstract class ReadOp extends Operation {

    public ReadOp(Timestamp t) {
        super(t);
    }
}