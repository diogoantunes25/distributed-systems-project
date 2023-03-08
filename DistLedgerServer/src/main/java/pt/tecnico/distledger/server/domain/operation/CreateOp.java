package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.server.visitor.Visitor;

public class CreateOp extends Operation {
    public CreateOp(String account) {
        super(account);
    }

    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
