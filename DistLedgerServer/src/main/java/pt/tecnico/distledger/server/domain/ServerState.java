package pt.tecnico.distledger.server.domain;

import pt.tecnico.distledger.server.domain.exceptions.AccountAlreadyExistsException;
import pt.tecnico.distledger.server.domain.exceptions.AccountDoesNotExistException;
import pt.tecnico.distledger.server.domain.exceptions.BalanceNotZeroException;
import pt.tecnico.distledger.server.domain.exceptions.NotEnoughBalanceException;
import pt.tecnico.distledger.server.domain.exceptions.ServerUnavailableException;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.DeleteOp;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.domain.operation.TransferOp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerState {
    private List<Operation> ledger;
    private boolean active;
    private Map<String, Account> accounts;

    public ServerState() {
        this.ledger = new ArrayList<>();
        this.active = true;

        this.accounts = new HashMap<>();
        Account broker = Account.getBroker();
        this.accounts.put(broker.getUserId(), broker);
    }

    public void createAccount(String userId)
        throws AccountAlreadyExistsException, ServerUnavailableException {
        if (!active) {
            throw new ServerUnavailableException();
        }

        if (accounts.containsKey(userId)) {
            throw new AccountAlreadyExistsException(userId);
        } 

        accounts.put(userId, new Account(userId));
        ledger.add(new CreateOp(userId));
    }

    public void deleteAccount(String userId)
        throws AccountDoesNotExistException, BalanceNotZeroException, ServerUnavailableException {
        if (!active) {
            throw new ServerUnavailableException();
        }

        if (!accounts.containsKey(userId)) {
            throw new AccountDoesNotExistException(userId);
        }

        Account user = accounts.get(userId);
        if (user.getBalance() != 0) {
            throw new BalanceNotZeroException(userId, user.getBalance());
        }

        accounts.remove(userId);
        ledger.add(new DeleteOp(userId));
    }

    public void transferTo(String accountFrom, String accoutTo, int amount)
        throws AccountDoesNotExistException, NotEnoughBalanceException, ServerUnavailableException {
        if (!active) {
            throw new ServerUnavailableException();
        }

        if (!accounts.containsKey(accountFrom)) {
            throw new AccountDoesNotExistException(accountFrom);
        }

        Account from = accounts.get(accountFrom);

        if (!accounts.containsKey(accoutTo)) {
            throw new AccountDoesNotExistException(accoutTo);
        }

        Account to = accounts.get(accountFrom);

        from.decreaseBalance(amount);
        to.increaseBalance(amount);
        ledger.add(new TransferOp(accountFrom, accountFrom, amount));
    }

    public int getBalance(String userId)
        throws AccountDoesNotExistException, ServerUnavailableException {
        if (!active) {
            throw new ServerUnavailableException();
        }

        if (!accounts.containsKey(userId)) {
            throw new AccountDoesNotExistException(userId);
        }

        return accounts.get(userId).getBalance();
    }

    public void activate() {
        active = true;
    }

    public void deactivate() {
        active = false;
    }

    public List<Operation> getLedgerState() {
        return ledger;
    }
}
