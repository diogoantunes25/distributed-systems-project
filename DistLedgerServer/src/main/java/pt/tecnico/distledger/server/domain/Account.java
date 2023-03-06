package pt.tecnico.distledger.server.domain;

import pt.tecnico.distledger.server.domain.exceptions.NotEnoughBalanceException;

public class Account {
    private int balance;
    private String userId;

    public static final int TOTAL_COIN = 1000;
    public static final String BROKER_ID = "__BROKER__";

    private Account(String userId, int initialBalance) {
        this.userId = userId;
        this.balance = initialBalance;
    }

    public Account(String userId) {
        this(userId, 0);
    }

    public static Account getBroker() {
        return new Account(BROKER_ID, TOTAL_COIN);
    }

    public void increaseBalance(int amount) {
        this.balance += amount;
    }

    public void decreaseBalance(int amount) throws NotEnoughBalanceException {
        if (amount <= balance) {
            this.balance -= amount;
            return;
        }
        throw new NotEnoughBalanceException(balance, amount);
    }

    public int getBalance() {
        return balance;
    }

    public String getUserId() {
        return userId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Account) {
            Account a = (Account) obj; 
            return a.userId.compareTo(userId) == 0;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return userId.hashCode();
    }
}