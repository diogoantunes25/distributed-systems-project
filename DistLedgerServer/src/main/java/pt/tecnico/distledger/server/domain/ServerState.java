package pt.tecnico.distledger.server.domain;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import pt.tecnico.distledger.gossip.Timestamp;
import pt.tecnico.distledger.server.domain.exceptions.*;
import pt.tecnico.distledger.server.domain.operation.*;
import pt.tecnico.distledger.server.exceptions.OperationAlreadyExecutedException;
import pt.tecnico.distledger.server.visitor.ExecutorVisitor;
import pt.tecnico.distledger.server.visitor.Visitor;

public class ServerState {
    volatile private String target;
    volatile private List<UpdateOp> ledger;
    volatile private int minStable;
    volatile private AtomicBoolean active;
    volatile private Map<String, Account> accounts;
    volatile private Timestamp replicaTS;
    volatile private Timestamp valueTS;
    private ReentrantLock lock;
    private Condition condition;
    private Thread worker;

    public ServerState(String target) {
        this.target = target;
        this.active = new AtomicBoolean(true);
        this.valueTS = new Timestamp();
        this.replicaTS = new Timestamp();
        
        this.ledger = new ArrayList<>();
        this.minStable = 0;

        // Lock for the ledger
        this.lock = new ReentrantLock();
        this.condition = lock.newCondition();

        // Single thread looping over the updates, trying to execute them
        this.worker = new Thread(() -> {
            try {
                tryProgress();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        this.worker.start();

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

    public Timestamp getValueTS() {
        return valueTS;
    }

    public boolean isStable(Timestamp t) {
        return Timestamp.lessOrEqual(t, valueTS);
    }

    public Read<List<UpdateOp>> getLedgerCopy() {
        try {
            lock.lock();
            List<UpdateOp> copy = new ArrayList<>();
            for (UpdateOp op: ledger) copy.add(op.getCopy());
            return new Read<>(copy, replicaTS.getCopy());
        } finally {
            lock.unlock();
        }
    }

    // Public operations
    public Timestamp createAccount(UpdateId updateId, String account, Timestamp prev)
            throws ServerUnavailableException, OperationAlreadyExecutedException {
        assertIsActive();
        Timestamp ts = replicaTS.increaseAndGetCopy(target);
        addUpdate(new CreateOp(prev, prev.getCopy().set(target, ts.getTime(target)), updateId, account));

        return ts;
    }

    public Timestamp transferTo(UpdateId updateId, String accountFrom, String accountTo, int amount, Timestamp prev)
            throws ServerUnavailableException, OperationAlreadyExecutedException {
        assertIsActive();
        Timestamp ts = replicaTS.increaseAndGetCopy(target);
        addUpdate(new TransferOp(prev, prev.getCopy().set(target, ts.getTime(target)), updateId, accountFrom, accountTo, amount));
        return ts;
    }

    public Read<Integer> getBalance(String userId, Timestamp prev)
            throws ServerUnavailableException, RuntimeException {
        assertIsActive();
        try {
            return read(new GetBalanceOp(prev, userId));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
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

    public Read<List<UpdateOp>> getLedgerState(Timestamp prev) 
            throws RuntimeException {
        assertIsActive();
        try {
            return read(new GetLedgerOp(prev));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
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

    private <T> Read<T> read(ReadOp op) throws InterruptedException {
        try {
            lock.lock();
            while(!isStable(op.getPrev())){
                condition.await();
            }
            Visitor<T> visitor = new ExecutorVisitor<>(this);
            return new Read<T>(op.accept(visitor), valueTS.getCopy());
        } finally {
            lock.unlock();
        }
    }

    // Only one thread executes this function
    private void tryProgress() throws InterruptedException {

        Visitor<Void> visitor = new ExecutorVisitor<>(this);
        try {
            int ledgerSize = ledger.size();
            while (true) {
                boolean updated = false;
                for (int i = minStable; i < ledgerSize; i++) {
                    UpdateOp op = ledger.get(i);

                    // Check if op was already stable
                    if (op.isStable()) {
                        System.out.printf("[ServerState] update %s was already stable, skipping\n", op.getUid().getUid());
                        continue;
                    }

                    // Check if op is still not stable
                    if (!isStable(op.getPrev())) {
                        System.out.printf("[ServerState] update %s is not stable, skipping\n", op.getUid().getUid());
                        continue;
                    }

                    // op has recently turned stable, execute it
                    System.out.printf("[ServerState] Update %s is now stable, running.\n", op.getUid().getUid());
                    op.setStable();

                    synchronized (valueTS) {
                        try {
                            op.accept(visitor);
                        } catch (DistLedgerRuntimeException e) {
                            System.out.println("Found invalid operation");
                        } finally {
                            this.valueTS.merge(op.getTs());
                            updated = true;
                            if (i == minStable) minStable++;
                        }
                    }
                }

                if (updated) continue;

                lock.lock();
                while (ledgerSize == ledger.size()) {
                    condition.await();
                    System.out.printf("[ServerState] await interrupted\n");
                }
                System.out.printf("[ServerState] conditions changed, moving on\n");
                ledgerSize = ledger.size();
                lock.unlock();
            }
        } finally {
            if (lock.isLocked()) lock.unlock();
        }

    }

    public void _createAccount(String userId)
            throws AccountAlreadyExistsException, ServerUnavailableException {
        assertCanCreateAccount(userId);
        accounts.put(userId, new Account(userId));
        System.out.printf("[ServerState] Created account with id %s\n", userId);
    }

    public void _transferTo(String accountFrom, String accountTo, int amount)
            throws AccountDoesNotExistException, NotEnoughBalanceException, ServerUnavailableException,
            InvalidTransferAmountException {
        assertCanTransferTo(accountFrom, accountTo, amount);
        accounts.get(accountFrom).decreaseBalance(amount);
        accounts.get(accountTo).increaseBalance(amount);
        System.out.printf("[ServerState] Transferred %s from %s to %s\n", amount, accountFrom, accountTo);
    }

    public Integer _getBalance(String userId)
            throws AccountDoesNotExistException, ServerUnavailableException {
        assertCanGetBalance(userId);
        System.out.printf("[ServerState] Got balance for %s - is %s \n", userId, accounts.get(userId).getBalance());
        return accounts.get(userId).getBalance();
    }

    public List<UpdateOp> _getLedgerState() {
        List<UpdateOp> ledgerCopy = new ArrayList<>();
        for (UpdateOp op : ledger) {
            ledgerCopy.add(op);
        }
        return ledgerCopy;
    }

    private boolean executed(UpdateOp op) {
        for (UpdateOp operation: ledger) 
            if (operation.getUid().equals(op.getUid())) return true;
        return false;
    }

    // Returns new replica TS
    public void addUpdate(UpdateOp op) throws OperationAlreadyExecutedException {
        if (executed(op)) throw new OperationAlreadyExecutedException(op.getUid());

        try {
            lock.lock();
            System.out.printf("[ServerState] update %s added to log\n", op.getUid().getUid());
            ledger.add(op);
            condition.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Merges log from other replica into current log
     * @param log incoming log
     * @param peerTS incoming timestamp
     */
    public void mergeLog(List<UpdateOp> log, Timestamp peerTS) {
        try {
            lock.lock();
            log.stream().filter(op -> !Timestamp.lessOrEqual(op.getTs(), replicaTS)) // Remove ops already in log
                    .sorted(((o1, o2) ->
                        Timestamp.getTotalOrderPrevComparator().compare(o1.getPrev(), o2.getPrev())
                    )) // Sort by prev value
                    .forEach(op1 -> {
                        try {
                            addUpdate(op1);
                        } catch (OperationAlreadyExecutedException e) {}
                    }); // If ops are put consecutively into ledger sorted, they will be executed respecting the partial order

            replicaTS.merge(peerTS);
            condition.signal();
        } finally {
            lock.unlock();
        }

    }

    public class Read<T> {
        private T value;
        private Timestamp newTs;

        public Read(T value, Timestamp t) {
            System.out.printf("[Read] new read with value %s\n", value);
            this.value = value;
            this.newTs = t;
        }

        public T getValue() {
            return value;
        }

        public Timestamp getNewTs() {
            return newTs;
        }
    }
}