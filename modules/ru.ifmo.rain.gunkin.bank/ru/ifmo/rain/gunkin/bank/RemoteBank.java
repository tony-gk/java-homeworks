package ru.ifmo.rain.gunkin.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteBank extends Remote {
    /**
     * Creates a new account with specified identifier if it is not already exists.
     * @param id account id
     * @return created or existing account.
     */
    RemoteAccount createAccount(String id) throws RemoteException;

    /**
     * Returns account by identifier.
     * @param id account id
     * @return account with specified identifier or {@code null} if such account does not exists.
     */
    RemoteAccount getAccount(String id) throws RemoteException;
}
