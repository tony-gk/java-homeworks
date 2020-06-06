package ru.ifmo.rain.gunkin.bank.server;

import ru.ifmo.rain.gunkin.bank.common.Account;
import ru.ifmo.rain.gunkin.bank.common.Person;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RemotePerson implements Person {
    private final String firstName;
    private final String secondName;
    private final String passportId;
    private final int port;
    private final ConcurrentMap<String, RemoteAccount> accounts;

    public RemotePerson(String firstName, String secondName, String passportId, int port) {
        this.firstName = firstName;
        this.secondName = secondName;
        this.passportId = passportId;
        this.port = port;
        this.accounts = new ConcurrentHashMap<>();
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
    public Account createAccount(String subId) throws RemoteException {
        String accountId = passportId + ":" + subId;
        RemoteAccount account = new RemoteAccount(accountId);

        if (accounts.putIfAbsent(subId, account) == null) {
            UnicastRemoteObject.exportObject(account, port);
            return account;
        } else {
            return getAccount(subId);
        }
    }

    @Override
    public Set<String> getAccountSubIds() {
        return accounts.keySet();
    }

}
