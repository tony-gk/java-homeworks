package ru.ifmo.rain.gunkin.bank.test;

import org.junit.*;
import org.junit.runners.MethodSorters;
import ru.ifmo.rain.gunkin.bank.common.Account;
import ru.ifmo.rain.gunkin.bank.common.Bank;
import ru.ifmo.rain.gunkin.bank.common.Person;
import ru.ifmo.rain.gunkin.bank.server.RemoteBank;

import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;


import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BankTest {
    private static final int PORT = 8888;
    private static final String BANK_URL = "//localhost/bank";
    private static Bank bank;

    private static final String FIRST_NAME = "FirstName";
    private static final String SECOND_NAME = "SecondName";
    private static final String PASSPORT_ID = "849234120";
    private static final String SUB_ID = "389234";
    private static final String ACCOUNT_ID = PASSPORT_ID + ":" + SUB_ID;

    private static Registry registry;

    @BeforeClass
    public static void startRmi() throws RemoteException {
        try {
            registry = LocateRegistry.createRegistry(PORT);
        } catch (RemoteException e) {
            registry = LocateRegistry.getRegistry(PORT);
        }
    }

    @AfterClass
    public static void stopRmi() throws NoSuchObjectException {
        UnicastRemoteObject.unexportObject(registry, true);
    }

    @Before
    public void createBank() throws RemoteException, NotBoundException {
        Bank remoteBank = new RemoteBank(PORT);
        UnicastRemoteObject.exportObject(remoteBank, PORT);
        registry.rebind(BANK_URL, remoteBank);

        bank = (Bank) registry.lookup(BANK_URL);
    }

    @Test
    public void test01_testPersonRegister() throws RemoteException {
        assertNull("Person is already registered in the new bank", bank.getRemotePerson(PASSPORT_ID));

        Person person = bank.registerPerson(FIRST_NAME, SECOND_NAME, PASSPORT_ID);

        checkPerson(person);
        checkPerson(bank.getLocalPerson(PASSPORT_ID));
        checkPerson(bank.getRemotePerson(PASSPORT_ID));
    }

    @Test
    public void test02_testBankAccountCreate() throws RemoteException {
        bank.registerPerson(FIRST_NAME, SECOND_NAME, PASSPORT_ID);
        Person remotePerson = bank.getRemotePerson(PASSPORT_ID);

        bank.createAccount(SUB_ID, PASSPORT_ID);
        checkAccountCreated(remotePerson);
    }

    @Test
    public void test03_testRemotePersonAccountCreate() throws RemoteException {
        Person person = bank.registerPerson(FIRST_NAME, SECOND_NAME, PASSPORT_ID);

        person.createAccount(SUB_ID);
        checkAccountCreated(person);
    }

    @Test
    public void test04_testLocalPersonAccountCreate() throws RemoteException {
        bank.registerPerson(FIRST_NAME, SECOND_NAME, PASSPORT_ID);

        Person localPerson = bank.getLocalPerson(PASSPORT_ID);
        localPerson.createAccount(SUB_ID);

        checkAccount(localPerson.getAccount(SUB_ID));
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

    private void checkAccountCreated(Person remotePerson) throws RemoteException {
        Account bankAccount = bank.getAccount(SUB_ID, remotePerson.getPassportId());
        Account remoteAccount = remotePerson.getAccount(SUB_ID);
        Account localAccount = bank.getLocalPerson(remotePerson.getPassportId()).getAccount(SUB_ID);

        checkAccount(bankAccount);
        checkAccount(remoteAccount);
        checkAccount(localAccount);
    }

    private void checkPerson(Person person) throws RemoteException {
        assertNotNull(person);
        assertEquals(person.getFirstName(), FIRST_NAME);
        assertEquals(person.getSecondName(), SECOND_NAME);
        assertEquals(person.getPassportId(), PASSPORT_ID);
    }

    private void checkAccount(Account account) throws RemoteException {
        assertNotNull(account);
        assertEquals(account.getId(), ACCOUNT_ID);
        assertEquals(account.getAmount(), 0);
    }
}
