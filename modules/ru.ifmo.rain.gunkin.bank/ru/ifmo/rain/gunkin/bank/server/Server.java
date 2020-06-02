package ru.ifmo.rain.gunkin.bank.server;

import ru.ifmo.rain.gunkin.bank.common.Bank;
import ru.ifmo.rain.gunkin.bank.server.RemoteBank;

import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.*;

public class Server {
    private final static int PORT = 8888;

    public static void main(final String... args) {
        Registry registry;
        try {
            registry = LocateRegistry.createRegistry(PORT);
        } catch (RemoteException e) {
            System.out.println("Cannot create registry: " + e.getMessage());
            return;
        }

        final Bank bank = new RemoteBank(PORT);
        try {
            UnicastRemoteObject.exportObject(bank, PORT);
//            Naming.rebind("//localhost/bank", bank);
            registry.rebind("//localhost/bank", bank);
        } catch (final RemoteException e) {
            System.out.println("Cannot export object: " + e.getMessage());
            e.printStackTrace();
        }
//        catch (final MalformedURLException e) {
//            System.out.println("Malformed URL");
//        }
        System.out.println("Server started");
    }
}
