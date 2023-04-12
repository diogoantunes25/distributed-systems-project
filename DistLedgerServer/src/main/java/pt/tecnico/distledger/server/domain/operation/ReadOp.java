package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.gossip.Timestamp;

public abstract class ReadOp extends Operation {

    public ReadOp(Timestamp t) {
        super(t);
    }
}