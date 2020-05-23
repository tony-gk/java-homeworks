package ru.ifmo.rain.gunkin.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Person extends Remote {
    String getFirstName()  throws RemoteException;

    String getSecondName() throws RemoteException;

    String getPassportId() throws RemoteException;

}
