package pt.tecnico.distledger.server.domain.exceptions;

import pt.tecnico.distledger.server.domain.ServerState;

public class InvalidLedgerException extends RuntimeException {
    private ServerState state;

    public InvalidLedgerException(ServerState state, Throwable cause) {
        super("Invalid ledger state provided", cause);
        this.state = state;
    }

    public InvalidLedgerException(ServerState state) {
        this.state = state;
    }

    public ServerState getState() {
        return state;
    }
}
