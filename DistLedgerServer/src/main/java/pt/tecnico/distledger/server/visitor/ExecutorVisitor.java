package pt.tecnico.distledger.server.visitor;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.exceptions.*;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.DeleteOp;
import pt.tecnico.distledger.server.domain.operation.TransferOp;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ExecutorVisitor implements Visitor<Void>{

    private ServerState state;

    public ExecutorVisitor(ServerState state) {
        this.state = state;
    }

    @Override
    public Void visit(CreateOp op) {
        try {
            state._createAccount(op.getAccount());
        } catch (AccountAlreadyExistsException e) {
            throw new InvalidLedgerException(state, e);
        }

        return null;
    }

    @Override
    public Void visit(DeleteOp op) {
        try {
            state._deleteAccount(op.getAccount());
        } catch (AccountDoesNotExistException | BalanceNotZeroException | BrokerCannotBeDeletedException e) {
            throw new InvalidLedgerException(state, e);
        }

        return null;
    }

    @Override
    public Void visit(TransferOp op) {
        try {
            state._transferTo(op.getAccount(), op.getDestAccount(), op.getAmount());
        } catch (AccountDoesNotExistException | NotEnoughBalanceException |
                 InvalidTransferAmountException e) {
            throw new InvalidLedgerException(state, e);
        }

        return null;
    }
}