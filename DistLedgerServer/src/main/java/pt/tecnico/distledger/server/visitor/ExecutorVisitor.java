package pt.tecnico.distledger.server.visitor;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.exceptions.*;
import pt.tecnico.distledger.server.domain.operation.*;

public class ExecutorVisitor<T> implements Visitor<T>{

    private ServerState state;

    public ExecutorVisitor(ServerState state) {
        this.state = state;
    }

    @Override
    public T visit(CreateOp op) {
        try {
            state._createAccount(op.getAccount());
        } catch (AccountAlreadyExistsException e) {
            throw new InvalidLedgerException(state, e);
        }

        return null;
    }

    @Override
    public T visit(TransferOp op) {
        try {
            state._transferTo(op.getAccount(), op.getDestAccount(), op.getAmount());
        } catch (AccountDoesNotExistException | NotEnoughBalanceException |
                 InvalidTransferAmountException e) {
            throw new InvalidLedgerException(state, e);
        }

        return null;
    }

    @Override
    public T visit(GetBalanceOp op) {
        try {
            return (T) state._getBalance(op.getUserId());
        } catch (AccountDoesNotExistException e) {
            throw new DistLedgerRuntimeException(String.format("Account for %s does not exist", op.getUserId()), e);
        }
    }

    @Override
    public T visit(GetLedgerOp op) {
        return (T) state._getLedgerState();
    }
}