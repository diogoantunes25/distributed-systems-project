package pt.tecnico.distledger.server.visitor;

import pt.tecnico.distledger.server.domain.operation.*;

public interface Visitor<T> {
    public T visit(CreateOp op);
    public T visit(DeleteOp op);
    public T visit(TransferOp op);
    public T visit(GetBalanceOp op);
    public T visit(GetLedgerOp op);
}
