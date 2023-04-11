package pt.tecnico.distledger.server.visitor;

import pt.tecnico.distledger.server.domain.operation.*;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.Operation;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.OperationType;

public class MessageConverterVisitor implements Visitor<Operation> {

    public Operation visit(CreateOp op) {
        return Operation.newBuilder()
                        .setType(OperationType.OP_CREATE_ACCOUNT)
                        .setUserId(op.getAccount())
                        .build();
    }

    public Operation visit(DeleteOp op) {
        return Operation.newBuilder()
                        .setType(OperationType.OP_DELETE_ACCOUNT)
                        .setUserId(op.getAccount())
                        .build();
    }

    public Operation visit(TransferOp op) {
        return Operation.newBuilder()
                        .setType(OperationType.OP_TRANSFER_TO)
                        .setUserId(op.getAccount())
                        .setDestUserId(op.getDestAccount())
                        .setAmount(op.getAmount())
                        .build();
    }

    @Override
    public Operation visit(GetBalanceOp op) {
        throw new RuntimeException();
    }

    @Override
    public Operation visit(GetLedgerOp op) {
        throw new RuntimeException();
    }
}