package pt.tecnico.distledger.server.domain;

import java.sql.Time;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

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
    private ExecutorVisitor<Void> updateVisitor = new ExecutorVisitor<>(this);
    private Set<UpdateId> executed = new HashSet<>();

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
        clientUpdate(new CreateOp(prev, updateId, account));

        return ts;
    }

    public Timestamp transferTo(UpdateId updateId, String accountFrom, String accountTo, int amount, Timestamp prev)
            throws ServerUnavailableException, OperationAlreadyExecutedException {
        assertIsActive();
        Timestamp ts = replicaTS.increaseAndGetCopy(target);
        clientUpdate(new TransferOp(prev, updateId, accountFrom, accountTo, amount));
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

    // Returns new replica TS
    private Timestamp clientUpdate(UpdateOp op) throws OperationAlreadyExecutedException {
        try {
            lock.lock();
            if (executed.contains(op.getUid())) throw new OperationAlreadyExecutedException(op.getUid());

            System.out.printf("[ServerState] update %s added to log\n", op.getUid().getUid());

            replicaTS.increase(this.target);
            Timestamp ts = op.getPrev().getCopy().set(target, replicaTS.getTime(target));
            op.setTs(ts);
            ledger.add(op);

            if (isStable(op.getPrev())) {
                op.accept(updateVisitor);
                op.setStable();
                condition.signalAll();

                valueTS.merge(op.getTs());

                executed.add(op.getUid());
            }

            return ts;
        } finally {
            lock.unlock();
        }
    }

    public void gossip(Timestamp otherTS, List<UpdateOp> ops) {
        try {
            lock.lock();
            replicaTS.merge(otherTS);

            // Add operations not yet executed
            ledger.addAll(ops.stream()
                .filter(op -> !Timestamp.lessOrEqual(op.getTs(), otherTS))
                .collect(Collectors.toList()));

            Comparator<Timestamp> timestampComparator = Timestamp.getTotalOrderPrevComparator();
            ledger.stream()
                    .filter(op -> !op.isStable() && isStable(op.getPrev())) // get operations that are now stable (but weren't)
                    .sorted((o1, o2) -> timestampComparator.compare(o1.getPrev(), o2.getPrev())) // sort by prev
                    .forEach(op -> {
                        op.accept(updateVisitor);
                        op.setStable();
                        valueTS.merge(op.getTs());
                        executed.add(op.getUid());
                    });

            condition.signalAll();
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