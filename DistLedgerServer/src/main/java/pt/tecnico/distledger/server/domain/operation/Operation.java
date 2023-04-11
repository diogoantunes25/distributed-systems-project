package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.gossip.Timestamp;
import pt.tecnico.distledger.server.visitor.Visitor;

public abstract class Operation {
    Timestamp prev;

    public Operation(Timestamp t) {
        this.prev = t;
    }

    public Timestamp getPrev() {
        return prev;
    }

    public abstract <T> T accept(Visitor<T> visitor);
}