package ru.ifmo.rain.gunkin.bank.server;

import ru.ifmo.rain.gunkin.bank.common.Account;
import ru.ifmo.rain.gunkin.bank.common.Person;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class LocalPerson implements Person, Serializable {
    private final String firstName;
    private final String secondName;
    private final String passportId;
    private final ConcurrentMap<String, LocalAccount> accounts;

    public LocalPerson(RemotePerson remotePerson) throws RemoteException {
        this.firstName = remotePerson.getFirstName();
        this.secondName = remotePerson.getSecondName();
        this.passportId = remotePerson.getPassportId();
        this.accounts = new ConcurrentHashMap<>();

        for (String subId  : remotePerson.getAccountSubIds()) {
            Account remoteAccount = remotePerson.getAccount(subId);

            LocalAccount localAccount = new LocalAccount(remoteAccount.getId());
            localAccount.setAmount(remoteAccount.getAmount());
            accounts.put(subId, localAccount);
        }
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

    @Override
    public Set<String> getAccountSubIds() {
        return accounts.keySet();
    }
}
