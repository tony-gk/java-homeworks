package ru.ifmo.rain.gunkin.bank.server;

import ru.ifmo.rain.gunkin.bank.common.Account;
import ru.ifmo.rain.gunkin.bank.common.Bank;
import ru.ifmo.rain.gunkin.bank.common.Person;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RemoteBank implements Bank {
    private final int port;
    private final ConcurrentMap<String, RemoteAccount> accounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, RemotePerson> persons = new ConcurrentHashMap<>();

    public RemoteBank(int port) {
        this.port = port;
    }

    @Override
    public Account createAccount(String subId, String passportId) throws RemoteException {
        if (persons.get(passportId) == null) {
            throw new IllegalArgumentException("A person with specified passport id is not registered");
        }

        String accountId = passportId + ":" + subId;
        RemoteAccount account = new RemoteAccount(accountId);

        if (accounts.putIfAbsent(accountId, account) == null) {
            UnicastRemoteObject.exportObject(account, port);
            return account;
        } else {
            return getAccount(subId, passportId);
        }
    }


    @Override
    public Account getAccount(String subId, String passportId) {
        String accountId = passportId + ":" + subId;
        return accounts.get(accountId);
    }

    @Override
    public Person registerPerson(String firstName, String secondName, String passportId) throws RemoteException {
        RemotePerson person = new RemotePerson(firstName, secondName, passportId, this);
        if (persons.putIfAbsent(passportId, person) == null) {
            UnicastRemoteObject.exportObject(person, port);
            return person;
        } else {
            return getRemotePerson(passportId);
        }
    }

    @Override
    public Person getLocalPerson(String passportId) {
        RemotePerson remotePerson = persons.get(passportId);
        if (remotePerson == null) {
            return null;
        }

        Map<String, LocalAccount> personAccounts = new HashMap<>();

        for (Map.Entry<String, RemoteAccount> e : accounts.entrySet()) {
            String accountId = e.getKey();
            if (accountId.startsWith(passportId)) {
                LocalAccount localAccount = new LocalAccount(e.getValue());
                personAccounts.put(
                        accountId.substring(accountId.indexOf(':') + 1),
                        localAccount);
            }
        }

        return new LocalPerson(remotePerson, personAccounts);
    }

    @Override
    public Person getRemotePerson(String passportId) {
        return persons.get(passportId);
    }
}
