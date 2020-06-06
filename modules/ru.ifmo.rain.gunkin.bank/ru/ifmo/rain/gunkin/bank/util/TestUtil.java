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

    static public void checkAccountCreated(String passportId, String subId, Bank bank, int amount) throws RemoteException {
        Account bankAccount = bank.getAccount(subId, passportId);
        Account remoteAccount = bank.getRemotePerson(passportId).getAccount(subId);
        Account localAccount = bank.getLocalPerson(passportId).getAccount(subId);

        String accountId = passportId + ":" + subId;

        checkAccount(bankAccount, accountId, amount);
        checkAccount(remoteAccount, accountId, amount);
        checkAccount(localAccount, accountId, amount);
    }


    static public void checkAccount(Account account, String accountId, int amount) throws RemoteException {
        assertNotNull(account);
        assertEquals(account.getId(), accountId);
        assertEquals(account.getAmount(), amount);
    }
}
