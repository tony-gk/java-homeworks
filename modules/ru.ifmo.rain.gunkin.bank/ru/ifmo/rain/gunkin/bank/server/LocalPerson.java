package ru.ifmo.rain.gunkin.bank.server;

import ru.ifmo.rain.gunkin.bank.common.Account;
import ru.ifmo.rain.gunkin.bank.common.Person;

import java.io.Serializable;
import java.util.Map;

public class LocalPerson implements Person, Serializable {
    private final String firstName;
    private final String secondName;
    private final String passportId;
    private final Map<String, LocalAccount> accounts;

    public LocalPerson(RemotePerson other, Map<String, LocalAccount> accounts) {
        this.firstName = other.getFirstName();
        this.secondName = other.getSecondName();
        this.passportId = other.getPassportId();
        this.accounts = accounts;
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    @Override
    public String getSecondName() {
        return secondName;
    }

    @Override
    public String getPassportId() {
        return passportId;
    }

    @Override
    public Account getAccount(String subId) {
        return accounts.get(subId);
    }

    @Override
    public Account createAccount(String subId) {
        LocalAccount account = accounts.get(subId);
        if (account == null) {
            account = new LocalAccount(passportId + ":" + subId);
            accounts.put(subId, account);
        }
        return account;
    }
}
