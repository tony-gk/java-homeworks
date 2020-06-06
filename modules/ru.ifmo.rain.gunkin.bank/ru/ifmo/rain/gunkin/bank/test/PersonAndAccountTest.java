package ru.ifmo.rain.gunkin.bank.test;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import ru.ifmo.rain.gunkin.bank.common.Account;
import ru.ifmo.rain.gunkin.bank.common.Person;

import java.rmi.RemoteException;

import static org.junit.Assert.*;
import static ru.ifmo.rain.gunkin.bank.util.TestUtil.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PersonAndAccountTest extends BaseTest {

    @Test
    public void test01_testPersonRegister() throws RemoteException {
        assertNull("Person is already registered in the new bank", bank.getRemotePerson(PASSPORT_ID));

        Person person = bank.registerPerson(FIRST_NAME, SECOND_NAME, PASSPORT_ID);

        checkPerson(person, FIRST_NAME, SECOND_NAME, PASSPORT_ID);
        checkPerson(bank.getLocalPerson(PASSPORT_ID), FIRST_NAME, SECOND_NAME, PASSPORT_ID);
        checkPerson(bank.getRemotePerson(PASSPORT_ID), FIRST_NAME, SECOND_NAME, PASSPORT_ID);
    }

    @Test
    public void test02_testBankAccountCreate() throws RemoteException {
        bank.registerPerson(FIRST_NAME, SECOND_NAME, PASSPORT_ID);

        bank.createAccount(SUB_ID, PASSPORT_ID);
        checkAccountCreated(PASSPORT_ID, SUB_ID, bank, 0);
    }

    @Test
    public void test03_testRemotePersonAccountCreate() throws RemoteException {
        Person person = bank.registerPerson(FIRST_NAME, SECOND_NAME, PASSPORT_ID);

        person.createAccount(SUB_ID);
        checkAccountCreated(PASSPORT_ID, SUB_ID, bank, 0);
    }

    @Test
    public void test04_testLocalPersonAccountCreate() throws RemoteException {
        bank.registerPerson(FIRST_NAME, SECOND_NAME, PASSPORT_ID);

        Person localPerson = bank.getLocalPerson(PASSPORT_ID);
        localPerson.createAccount(SUB_ID);

        checkAccount(localPerson.getAccount(SUB_ID), ACCOUNT_ID, 0);
    }

    @Test
    public void test05_testAccountSetAmount() throws RemoteException {
        Account remoteAccount = bank
                .registerPerson(FIRST_NAME, SECOND_NAME, PASSPORT_ID)
                .createAccount(SUB_ID);

        int amount = 200;
        remoteAccount.setAmount(200);

        assertEquals(remoteAccount.getAmount(), amount);
        assertEquals(bank.getAccount(SUB_ID, PASSPORT_ID).getAmount(), amount);
    }

    @Test
    public void test06_testLocalAccountCreateVisibility() throws RemoteException {
        bank.registerPerson(FIRST_NAME, SECOND_NAME, PASSPORT_ID);

        Person remotePerson = bank.getRemotePerson(PASSPORT_ID);
        Person localPerson = bank.getLocalPerson(PASSPORT_ID);
        localPerson.createAccount(SUB_ID);

        assertNotNull(localPerson.getAccount(SUB_ID));

        assertNull(remotePerson.getAccount(SUB_ID));
        assertNull(bank.getAccount(SUB_ID, PASSPORT_ID));
    }

    @Test
    public void test07_testLocalAccountSetAmountVisibility() throws RemoteException {
        bank.registerPerson(FIRST_NAME, SECOND_NAME, PASSPORT_ID).createAccount(SUB_ID);

        Account localAccount = bank.getLocalPerson(PASSPORT_ID).getAccount(SUB_ID);
        int oldAmount = localAccount.getAmount();
        int newAmount = 200;

        localAccount.setAmount(newAmount);

        Account remoteAccount = bank.getAccount(SUB_ID, PASSPORT_ID);

        assertEquals(localAccount.getAmount(), newAmount);
        assertEquals(remoteAccount.getAmount(), oldAmount);
    }

}
