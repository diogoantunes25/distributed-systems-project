package pt.tecnico.distledger.server.visitor;

import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.DeleteOp;
import pt.tecnico.distledger.server.domain.operation.TransferOp;

public interface Visitor<T> {
    public T visit(CreateOp op);
    public T visit(DeleteOp op);
    public T visit(TransferOp op);
}
