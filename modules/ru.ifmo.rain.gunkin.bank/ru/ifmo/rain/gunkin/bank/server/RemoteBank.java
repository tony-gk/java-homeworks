package ru.ifmo.rain.gunkin.bank.server;

import ru.ifmo.rain.gunkin.bank.common.Account;
import ru.ifmo.rain.gunkin.bank.common.Bank;
import ru.ifmo.rain.gunkin.bank.common.Person;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RemoteBank implements Bank {
    private final int port;
    private final ConcurrentMap<String, RemotePerson> persons = new ConcurrentHashMap<>();

    public RemoteBank(int port) {
        this.port = port;
    }

    @Override
    public Account createAccount(String subId, String passportId) throws RemoteException {
        Person person = persons.get(passportId);
        if (person == null) {
            throw new IllegalArgumentException("A person with specified passport id is not registered");
        }

        return person.createAccount(subId);
    }


    @Override
    public Account getAccount(String subId, String passportId) throws RemoteException {
        Person person = persons.get(passportId);
        if (person == null) {
            throw new IllegalArgumentException("A person with specified passport id is not registered");
        }
        return person.getAccount(subId);
    }

    @Override
    public Person registerPerson(String firstName, String secondName, String passportId) throws RemoteException {
        RemotePerson person = new RemotePerson(firstName, secondName, passportId, port);
        if (persons.putIfAbsent(passportId, person) == null) {
            UnicastRemoteObject.exportObject(person, port);
            return person;
        } else {
            return getRemotePerson(passportId);
        }
    }

    @Override
    public Person getLocalPerson(String passportId) throws RemoteException {
        RemotePerson person = persons.get(passportId);
        if (person == null) {
            return null;
        }
        return new LocalPerson(person);
    }

    @Override
    public Person getRemotePerson(String passportId) {
        return persons.get(passportId);
    }
}
