package ru.ifmo.rain.gunkin.bank.server;

import ru.ifmo.rain.gunkin.bank.common.Bank;
import ru.ifmo.rain.gunkin.bank.util.ServerUtil;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;


public class Server {
    public static void main(final String... args) throws RemoteException {
        Registry registry = LocateRegistry.createRegistry(ServerUtil.PORT);

        Bank bank = new RemoteBank(ServerUtil.PORT);
        UnicastRemoteObject.exportObject(bank, ServerUtil.PORT);
        registry.rebind(ServerUtil.BANK_URL, bank);

        System.out.println("Server started");
    }
}
