package ru.ifmo.rain.gunkin.bank;

public class RemoteAccount implements Account {
    private final String id;
    private int amount;

    public RemoteAccount(String id) {
        this.id = id;
        amount = 0;
    }

    public String getId() {
        return id;
    }

    public synchronized int getAmount() {
        System.out.println("Getting amount of money for remote account " + id);
        return amount;
    }

    public synchronized void setAmount(int amount) {
        System.out.println("Setting amount of money for remote account " + id);
        this.amount = amount;
    }
}
