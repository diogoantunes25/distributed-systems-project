package pt.tecnico.distledger.server.domain;

import pt.tecnico.distledger.server.domain.exceptions.AccountAlreadyExistsException;
import pt.tecnico.distledger.server.domain.exceptions.AccountDoesNotExistException;
import pt.tecnico.distledger.server.domain.exceptions.NotEnoughBalanceException;
import pt.tecnico.distledger.server.domain.operation.Operation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerState {
    private List<Operation> ledger;
    private boolean active;
    private Set<Account> accounts;

    public ServerState() {
        this.ledger = new ArrayList<>();
        this.active = true;
        this.accounts = new HashSet<>();
    }

    public void createAccount(String userId) throws AccountAlreadyExistsException {
        // TODO
    }

    public void deleteAccount(String userId) throws AccountDoesNotExistException {
        // TODO
    }

    public void transferTo(String accountFrom, String accoutTo, int amount)
        throws AccountDoesNotExistException, NotEnoughBalanceException {
        // TODO
    }

    public int getBalance(String userId) throws AccountDoesNotExistException {
        // TODO
        return -1;
    }

    public void activate() {
        // TODO
    }

    public void deactivate() {
        // TODO
    }

    public List<Operation> getLedgerState() {
        // TODO
        return null;
    }
}
