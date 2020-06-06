package ru.ifmo.rain.gunkin.bank.test;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import ru.ifmo.rain.gunkin.bank.common.Bank;
import ru.ifmo.rain.gunkin.bank.server.RemoteBank;

import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class BaseTest {
    private static final int PORT = 31231;
    private static final String BANK_URL = "//localhost/bank";
    protected static Bank bank;
    private static Registry registry;

    protected static final String FIRST_NAME = "FirstName";
    protected static final String SECOND_NAME = "SecondName";
    protected static final String PASSPORT_ID = "849234120";
    protected static final String SUB_ID = "389234";
    protected static final String ACCOUNT_ID = PASSPORT_ID + ":" + SUB_ID;

    @BeforeClass
    public static void startRmi() throws RemoteException {
        registry = LocateRegistry.createRegistry(PORT);
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

    @After
    public void removeBank() throws RemoteException, NotBoundException {
        registry.unbind(BANK_URL);
        UnicastRemoteObject.unexportObject(bank, true);
    }
}
