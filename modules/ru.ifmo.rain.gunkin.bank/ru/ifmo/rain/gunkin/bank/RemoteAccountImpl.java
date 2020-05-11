package ru.ifmo.rain.gunkin.bank;

public class RemoteAccountImpl implements RemoteAccount {
    private final String id;
    private int amount;

    public RemoteAccountImpl(final String id) {
        this.id = id;
        amount = 0;
    }

    public String getId() {
        return id;
    }

    public synchronized int getAmount() {
        System.out.println("Getting amount of money for account " + id);
        return amount;
    }

    public synchronized void setAmount(final int amount) {
        System.out.println("Setting amount of money for account " + id);
        this.amount = amount;
    }
}