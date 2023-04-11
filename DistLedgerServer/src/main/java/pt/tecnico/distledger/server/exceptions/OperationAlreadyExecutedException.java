package pt.tecnico.distledger.server.exceptions;

import pt.tecnico.distledger.server.domain.UpdateId;
import pt.tecnico.distledger.server.domain.exceptions.DistLedgerException;

public class OperationAlreadyExecutedException extends DistLedgerException {

    private UpdateId uid;

    public OperationAlreadyExecutedException(UpdateId uid) {
        this.uid = uid;
    }

    public UpdateId getUid() {
        return uid;
    }
}
