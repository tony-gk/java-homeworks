package ru.ifmo.rain.gunkin.bank.server;

import ru.ifmo.rain.gunkin.bank.common.Bank;
import ru.ifmo.rain.gunkin.bank.server.RemoteBank;

import java.net.PortUnreachableException;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.*;
import java.util.Objects;

public class Server {
    private static final String BANK_URL = "//localhost/bank";
    private static final int PORT = 8992;

    public static void main(final String... args) throws RemoteException {
        Registry registry = LocateRegistry.createRegistry(PORT);

        Bank bank = new RemoteBank(PORT);
        UnicastRemoteObject.exportObject(bank, PORT);
        registry.rebind(BANK_URL, bank);

        System.out.println("Server started");
    }
}
