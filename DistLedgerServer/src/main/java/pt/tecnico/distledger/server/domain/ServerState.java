package pt.tecnico.distledger.server.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import pt.tecnico.distledger.server.domain.exceptions.*;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.DeleteOp;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.tecnico.distledger.server.visitor.ExecutorVisitor;
import pt.tecnico.distledger.gossip.Timestamp;

public class ServerState {
    volatile private String target;
    volatile private AtomicBoolean active;
    volatile private List<Operation> ledger;
    volatile private Map<String, Account> accounts;
    volatile private Timestamp replicaTS;
    volatile private Timestamp valueTS;

    public ServerState(String target) {
        this.target = target;
        this.ledger = new ArrayList<>();
        this.active = new AtomicBoolean(true);

        this.accounts = new HashMap<>();
        Account broker = Account.getBroker();
        this.accounts.put(broker.getUserId(), broker);
    }

    public String getTarget() {
        return target;
    }

    public Timestamp getReplicaTS() {
        return replicaTS;
    }

    public void setReplicaTS(Timestamp replicaTS) {
        this.replicaTS = replicaTS;
    }

    public Timestamp getValueTS() {
        return valueTS;
    }

    public void setValueTS(Timestamp valueTS) {
        this.valueTS = valueTS;
    }

    public void assertIsActive() throws ServerUnavailableException {
        if (!active.get()) {
            throw new ServerUnavailableException();
        }
    }

    public synchronized void createAccount(String userId)
            throws AccountAlreadyExistsException, ServerUnavailableException {
        assertIsActive();
        assertCanCreateAccount(userId);
        accounts.put(userId, new Account(userId));
        ledger.add(new CreateOp(userId));
    }

    public synchronized void assertCanCreateAccount(String userId)
            throws AccountAlreadyExistsException {

        if (accounts.containsKey(userId)) {
            throw new AccountAlreadyExistsException(userId);
        }
    }

    public synchronized void deleteAccount(String userId)
            throws AccountDoesNotExistException, BalanceNotZeroException,
            ServerUnavailableException, BrokerCannotBeDeletedException {
        assertIsActive();
        assertCanDeleteAccount(userId);
        accounts.remove(userId);
        ledger.add(new DeleteOp(userId));
    }

    public synchronized void assertCanDeleteAccount(String userId) throws AccountDoesNotExistException, BalanceNotZeroException,
            BrokerCannotBeDeletedException {

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
    }

    public synchronized void transferTo(String accountFrom, String accountTo, int amount)
            throws AccountDoesNotExistException, NotEnoughBalanceException, ServerUnavailableException,
            InvalidTransferAmountException {
        assertIsActive();
        assertCanTransferTo(accountFrom, accountTo, amount);
        accounts.get(accountFrom).decreaseBalance(amount);
        accounts.get(accountTo).increaseBalance(amount);
        ledger.add(new TransferOp(accountFrom, accountTo, amount));
    }

    public synchronized void assertCanTransferTo(String accountFrom, String accountTo, int amount)
            throws AccountDoesNotExistException, NotEnoughBalanceException, InvalidTransferAmountException {
        if (!accounts.containsKey(accountFrom)) {
            throw new AccountDoesNotExistException(accountFrom);
        }

        if (!accounts.containsKey(accountTo)) {
            throw new AccountDoesNotExistException(accountTo);
        }

        if (amount <= 0) {
            throw new InvalidTransferAmountException(amount);
        }

        if (accounts.get(accountFrom).getBalance() < amount) {
            throw new NotEnoughBalanceException(accounts.get(accountFrom).getBalance(), amount);
        }
    }

    public synchronized int getBalance(String userId)
            throws AccountDoesNotExistException, ServerUnavailableException {
        assertIsActive();
        assertCanGetBalance(userId);
        return accounts.get(userId).getBalance();
    }

    public synchronized void assertCanGetBalance(String userId) throws AccountDoesNotExistException {
        if (!accounts.containsKey(userId)) {
            throw new AccountDoesNotExistException(userId);
        }
    }

    public synchronized void updateLedger(List<Operation> newOperations, int newStart) 
            throws ServerUnavailableException, InvalidLedgerException {
        assertIsActive();

        // Replay all actions
        ExecutorVisitor visitor = new ExecutorVisitor(this);
        for (Operation op : newOperations.subList(this.ledger.size() - newStart, newOperations.size()))
            op.accept(visitor);
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

    public synchronized List<Operation> getLedgerState() {
        List<Operation> ledgerCopy = new ArrayList<>();
        for (Operation op: ledger) {
            ledgerCopy.add(op);
        }
        return ledgerCopy;
    }
}
