package ru.ifmo.rain.gunkin.bank.server;

import ru.ifmo.rain.gunkin.bank.common.Account;

public abstract class AbstractAccount implements Account {
    protected final String id;
    protected int amount;

    protected AbstractAccount(String id) {
        this.id = id;
        this.amount = 0;
    }

    public String getId() {
        return id;
    }

    public synchronized int getAmount() {
        return amount;
    }

    public synchronized void setAmount(final int amount) {
        this.amount = amount;
    }

    public synchronized void addAmount(final int amount) {
        this.amount += amount;
    }
}
