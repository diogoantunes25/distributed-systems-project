package pt.tecnico.distledger.server.domain;

import pt.tecnico.distledger.server.domain.exceptions.AccountAlreadyExistsException;
import pt.tecnico.distledger.server.domain.exceptions.AccountDoesNotExistException;
import pt.tecnico.distledger.server.domain.exceptions.BalanceNotZeroException;
import pt.tecnico.distledger.server.domain.exceptions.BrokerCannotBeDeletedException;
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
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerState {
    volatile private List<Operation> ledger;
    volatile private AtomicBoolean active;
    volatile private Map<String, Account> accounts;

    public ServerState() {
        this.ledger = new ArrayList<>();
        this.active = new AtomicBoolean(true);

        this.accounts = new HashMap<>();
        Account broker = Account.getBroker();
        this.accounts.put(broker.getUserId(), broker);
    }

    public synchronized void createAccount(String userId)
        throws AccountAlreadyExistsException, ServerUnavailableException {
        if (!active.get()) {
            throw new ServerUnavailableException();
        }

        if (accounts.containsKey(userId)) {
            throw new AccountAlreadyExistsException(userId);
        } 

        accounts.put(userId, new Account(userId));
        ledger.add(new CreateOp(userId));
    }

    public synchronized void deleteAccount(String userId)
        throws AccountDoesNotExistException, BalanceNotZeroException,
                ServerUnavailableException, BrokerCannotBeDeletedException {
        if (!active.get()) {
            throw new ServerUnavailableException();
        }

        if (!accounts.containsKey(userId)) {
            throw new AccountDoesNotExistException(userId);
        }

        if (userId == Account.BROKER_ID) {
            throw new BrokerCannotBeDeletedException();
        }

        Account user = accounts.get(userId);
        if (user.getBalance() != 0) {
            throw new BalanceNotZeroException(userId, user.getBalance());
        }

        accounts.remove(userId);
        ledger.add(new DeleteOp(userId));
    }

    public synchronized void transferTo(String accountFrom, String accountTo, int amount)
        throws AccountDoesNotExistException, NotEnoughBalanceException, ServerUnavailableException {
        if (!active.get()) {
            throw new ServerUnavailableException();
        }

        if (!accounts.containsKey(accountFrom)) {
            throw new AccountDoesNotExistException(accountFrom);
        }

        if (!accounts.containsKey(accountTo)) {
            throw new AccountDoesNotExistException(accountTo);
        }

        accounts.get(accountFrom).decreaseBalance(amount);
        accounts.get(accountTo).increaseBalance(amount);
        ledger.add(new TransferOp(accountFrom, accountFrom, amount));
    }

    public synchronized int getBalance(String userId)
        throws AccountDoesNotExistException, ServerUnavailableException {
        if (!active.get()) {
            throw new ServerUnavailableException();
        }

        if (!accounts.containsKey(userId)) {
            throw new AccountDoesNotExistException(userId);
        }

        return accounts.get(userId).getBalance();
    }

    public Map<String, Account> getAccounts() {
        return accounts;
    }

    public void activate() {
        active.set(true);
    }

    public void deactivate() {
        active.set(false);
    }

    public List<Operation> getLedgerState() {
        return ledger;
    }
}
