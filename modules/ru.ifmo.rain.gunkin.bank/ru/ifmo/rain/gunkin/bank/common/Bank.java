package ru.ifmo.rain.gunkin.bank.common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Bank extends Remote {
    Account createAccount(String subId, String passportId) throws RemoteException;

    Account getAccount(String subId, String passportId) throws RemoteException;

    Person registerPerson(String firstName, String secondName, String passportId) throws RemoteException;

    Person getLocalPerson(String passportId) throws RemoteException;

    Person getRemotePerson(String passportId) throws RemoteException;

}
