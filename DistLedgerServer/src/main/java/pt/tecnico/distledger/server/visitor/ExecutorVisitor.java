package pt.tecnico.distledger.server.visitor;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.exceptions.*;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.DeleteOp;
import pt.tecnico.distledger.server.domain.operation.TransferOp;

public class ExecutorVisitor implements Visitor<Void>{

    private ServerState state;

    public ExecutorVisitor(ServerState state) {
        this.state = state;
    }

    @Override
    public Void visit(CreateOp op) {
        try {
            state.createAccount(op.getAccount());
        } catch (AccountAlreadyExistsException e) {
            e.printStackTrace();
            throw new InvalidLedgerException(state, e);
        }

        return null;
    }

    @Override
    public Void visit(DeleteOp op) {
        try {
            state.deleteAccount(op.getAccount());
        } catch (AccountDoesNotExistException | BalanceNotZeroException | BrokerCannotBeDeletedException e) {
            e.printStackTrace();
            throw new InvalidLedgerException(state, e);
        }

        return null;
    }

    @Override
    public Void visit(TransferOp op) {
        try {
            state.transferTo(op.getAccount(), op.getDestAccount(), op.getAmount());
        } catch (AccountDoesNotExistException | NotEnoughBalanceException |
                 InvalidTransferAmountException e) {
            e.printStackTrace();
            throw new InvalidLedgerException(state, e);
        }

        return null;
    }
}