package ru.ifmo.rain.gunkin.bank.util;

import ru.ifmo.rain.gunkin.bank.common.Account;
import ru.ifmo.rain.gunkin.bank.common.Bank;
import ru.ifmo.rain.gunkin.bank.common.Person;

import java.rmi.RemoteException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestUtil {
    static public void checkPerson(Person person, String firstName, String secondName, String passportId) throws RemoteException {
        assertNotNull(person);
        assertEquals(person.getFirstName(), firstName);
        assertEquals(person.getSecondName(), secondName);
        assertEquals(person.getPassportId(), passportId);
    }
    static public void checkAccountCreated(Person remotePerson, String subId, Bank bank) throws RemoteException {
        Account bankAccount = bank.getAccount(subId, remotePerson.getPassportId());
        Account remoteAccount = remotePerson.getAccount(subId);
        Account localAccount = bank.getLocalPerson(remotePerson.getPassportId()).getAccount(subId);

        String accountId = remotePerson.getPassportId()  + ":" + subId;

        checkAccount(bankAccount, accountId);
        checkAccount(remoteAccount, accountId);
        checkAccount(localAccount, accountId);
    }


    static public void checkAccount(Account account, String accountId) throws RemoteException {
        assertNotNull(account);
        assertEquals(account.getId(), accountId);
        assertEquals(account.getAmount(), 0);
    }
}
