package ru.ifmo.rain.gunkin.bank.main;

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
        String accountId = passportId + ":" + subId;
        System.out.println("Creating account in bank " + accountId);

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
        System.out.println("Retrieving account " + accountId);
        return accounts.get(accountId);
    }

    @Override
    public Person registerPerson(String firstName, String secondName, String passportId) throws RemoteException {
        System.out.println("Registering person " + passportId);

        RemotePerson person = new RemotePerson(firstName, secondName, passportId, this);
        if (persons.putIfAbsent(passportId, person) == null) {
            UnicastRemoteObject.exportObject(person, port);
            return person;
        } else {
            return getRemotePerson(passportId);
        }
    }

    @Override
    public Person getLocalPerson(String passportId) throws RemoteException {
        System.out.println("Retrieving local person " + passportId);

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
        System.out.println("Retrieving remote person " + passportId);
        return persons.get(passportId);
    }
}
