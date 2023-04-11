package pt.tecnico.distledger.server.domain;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import pt.tecnico.distledger.gossip.Timestamp;
import pt.tecnico.distledger.server.domain.exceptions.*;
import pt.tecnico.distledger.server.domain.operation.*;
import pt.tecnico.distledger.server.visitor.ExecutorVisitor;
import pt.tecnico.distledger.gossip.Timestamp;
import pt.tecnico.distledger.server.visitor.Visitor;

public class ServerState {
    volatile private String target;
    volatile private List<UpdateOp> ledger;
    volatile private AtomicBoolean active;
    volatile private Map<String, Account> accounts;
    volatile private Timestamp replicaTS;
    volatile private Timestamp valueTS;
    private ReentrantLock lock;
    private Condition condition;
    volatile private Timestamp stateTS;
    volatile private Set<UpdateOp> processed = ConcurrentHashMap.newKeySet();

    public ServerState(String target) {
        this.target = target;
        this.ledger = new ArrayList<>();
        this.active = new AtomicBoolean(true);
        this.lock = new ReentrantLock();
        this.condition = lock.newCondition();
        this.stateTS = new Timestamp();

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

    // Public operations

    public void createAccount(UpdateId updateId, String account, Timestamp prev)
            throws AccountAlreadyExistsException, ServerUnavailableException {
        assertIsActive();
        addUpdate(new CreateOp(prev, updateId, account));
    }

    public void deleteAccount(UpdateId updateId, String account, Timestamp prev)
            throws AccountDoesNotExistException, BalanceNotZeroException,
            ServerUnavailableException, BrokerCannotBeDeletedException {
        assertIsActive();
        addUpdate(new DeleteOp(prev, updateId, account));
    }

    public void transferTo(UpdateId updateId, String accountFrom, String accountTo, int amount, Timestamp prev)
            throws AccountDoesNotExistException, NotEnoughBalanceException, ServerUnavailableException,
            InvalidTransferAmountException {
        assertIsActive();
        addUpdate(new TransferOp(prev, updateId, accountFrom, accountTo, amount));
    }

    public int getBalance(String userId, Timestamp prev)
            throws AccountDoesNotExistException, ServerUnavailableException {
        assertIsActive();
        return read(new GetBalanceOp(prev, userId));
    }

    public void activate() {
        active.set(true);
    }

    public void deactivate() {
        active.set(false);
    }

    public Map<String, Account> getAccounts() {
        return accounts;
    }

    public List<Operation> getLedgerState(Timestamp prev) {
        assertIsActive();
        return read(new GetLedgerOp(prev));
    }

    // Assertions

    public void assertIsActive() throws ServerUnavailableException {
        if (!active.get()) {
            throw new ServerUnavailableException();
        }
    }

    public void assertCanCreateAccount(String userId)
            throws AccountAlreadyExistsException {

        if (accounts.containsKey(userId)) {
            throw new AccountAlreadyExistsException(userId);
        }
    }

    public void assertCanDeleteAccount(String userId) throws AccountDoesNotExistException, BalanceNotZeroException,
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

    public void assertCanTransferTo(String accountFrom, String accountTo, int amount)
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

    public void assertCanGetBalance(String userId) throws AccountDoesNotExistException {
        if (!accounts.containsKey(userId)) {
            throw new AccountDoesNotExistException(userId);
        }
    }

    private <T> T read(ReadOp op) {
        Visitor<T> visitor = new ExecutorVisitor<>(this);
        return op.accept(visitor);
    }

    // Only one thread executes this function
    private void tryProgress() throws InterruptedException {

        Visitor<Void> visitor = new ExecutorVisitor<>(this);
        try {
            int ledgerSize = ledger.size();
            while (true) {
                boolean updated = false;
                int i = 0;
                while (i < ledgerSize) {
                    UpdateOp op = ledger.get(i++);
                    if (op.isStable()) continue;
                    try {
                        op.accept(visitor);
                    } catch (DistLedgerRuntimeException e) {
                        System.out.println("Found invalid operation");
                        e.printStackTrace();
                    }
                    if (op.isStable()) updated = true;
                }

                if (updated) continue;

                lock.lock();
                while (ledgerSize == ledger.size()) {
                    condition.await();
                }
                ledgerSize = ledger.size();
                lock.unlock();
            }
        } finally {
            if (lock.isLocked()) lock.unlock();
        }

    }

    // Private getters and setters

    public void _createAccount(String userId)
            throws AccountAlreadyExistsException, ServerUnavailableException {
        assertCanCreateAccount(userId);
        accounts.put(userId, new Account(userId));
    }

    public void _deleteAccount(String userId)
            throws AccountDoesNotExistException, BalanceNotZeroException,
            ServerUnavailableException, BrokerCannotBeDeletedException {
        assertCanDeleteAccount(userId);
        accounts.remove(userId);
    }

    public void _transferTo(String accountFrom, String accountTo, int amount)
            throws AccountDoesNotExistException, NotEnoughBalanceException, ServerUnavailableException,
            InvalidTransferAmountException {
        assertCanTransferTo(accountFrom, accountTo, amount);
        accounts.get(accountFrom).decreaseBalance(amount);
        accounts.get(accountTo).increaseBalance(amount);
    }

    public Integer _getBalance(String userId)
            throws AccountDoesNotExistException, ServerUnavailableException {
        assertCanGetBalance(userId);
        return accounts.get(userId).getBalance();
    }

    public List<Operation> _getLedgerState() {
        List<Operation> ledgerCopy = new ArrayList<>();
        for (Operation op : ledger) {
            ledgerCopy.add(op);
        }
        return ledgerCopy;
    }

    public void addUpdate(UpdateOp op) {
        if (processed.contains(op)) return;
        processed.add(op);

        try {
            lock.lock();
            ledger.add(op);
            condition.signal();
        } finally {
            lock.unlock();
        }
    }
}