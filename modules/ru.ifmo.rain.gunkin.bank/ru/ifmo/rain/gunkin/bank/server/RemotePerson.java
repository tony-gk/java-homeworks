package ru.ifmo.rain.gunkin.bank.server;

import ru.ifmo.rain.gunkin.bank.common.Account;
import ru.ifmo.rain.gunkin.bank.common.Bank;
import ru.ifmo.rain.gunkin.bank.common.Person;

import java.rmi.RemoteException;

public class RemotePerson implements Person {
    private final String firstName;
    private final String secondName;
    private final String passportId;
    private final Bank bank;

    public RemotePerson(String firstName, String secondName, String passportId, Bank bank) {
        this.firstName = firstName;
        this.secondName = secondName;
        this.passportId = passportId;
        this.bank = bank;
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
    public Account getAccount(String subId) throws RemoteException {
        return bank.getAccount(subId, passportId);
    }

    @Override
    public Account createAccount(String subId) throws RemoteException {
        return bank.createAccount(subId, passportId);
    }
}
