package ru.ifmo.rain.gunkin.bank.common;

import java.rmi.*;

public interface Account extends Remote {
    String getId() throws RemoteException;

    int getAmount() throws RemoteException;

    void setAmount(int amount) throws RemoteException;

    void addAmount(int amount) throws RemoteException;
}