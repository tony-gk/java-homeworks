package ru.ifmo.rain.gunkin.bank.server;

import java.io.Serializable;

public class LocalAccount extends AbstractAccount implements Serializable {
    public LocalAccount(String id) {
        super(id);
    }

    public LocalAccount(RemoteAccount other) {
        super(other.getId());
        this.amount = other.getAmount();
    }
}
