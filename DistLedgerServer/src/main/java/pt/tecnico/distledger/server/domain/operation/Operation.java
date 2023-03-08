package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.server.visitor.Visitor;

public abstract class Operation {
    private String account;

    public Operation(String fromAccount) {
        this.account = fromAccount;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public abstract <T> T accept(Visitor<T> visitor);
}