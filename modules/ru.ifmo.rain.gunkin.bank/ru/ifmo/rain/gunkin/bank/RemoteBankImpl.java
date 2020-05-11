package ru.ifmo.rain.gunkin.bank;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RemoteBankImpl implements RemoteBank {
    private final int port;
    private final ConcurrentMap<String, RemoteAccount> accounts = new ConcurrentHashMap<>();

    public RemoteBankImpl(final int port) {
        this.port = port;
    }

    public RemoteAccount createAccount(final String id) throws RemoteException {
        System.out.println("Creating account " + id);
        final RemoteAccount account = new RemoteAccountImpl(id);
        if (accounts.putIfAbsent(id, account) == null) {
            UnicastRemoteObject.exportObject(account, port);
            return account;
        } else {
            return getAccount(id);
        }
    }

    public RemoteAccount getAccount(final String id) {
        System.out.println("Retrieving account " + id);
        return accounts.get(id);
    }
}
