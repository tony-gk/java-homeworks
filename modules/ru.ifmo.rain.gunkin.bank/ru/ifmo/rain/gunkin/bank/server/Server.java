package ru.ifmo.rain.gunkin.bank.server;

import ru.ifmo.rain.gunkin.bank.common.Bank;
import ru.ifmo.rain.gunkin.bank.util.ServerUtil;

import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.atomic.AtomicBoolean;


public class Server {
    private static Registry registry;
    private static Bank bank;
    private static final AtomicBoolean started = new AtomicBoolean(false);


    public static void start() throws ServerException {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("Server is already started");
        }

        bank = new RemoteBank(ServerUtil.PORT);
        try {
            registry = LocateRegistry.createRegistry(ServerUtil.PORT);
            UnicastRemoteObject.exportObject(bank, ServerUtil.PORT);
            registry.rebind(ServerUtil.BANK_URL, bank);
        } catch (RemoteException e) {
            started.set(false);
            throw new ServerException("Can't bind bank to registry", e);
        }

        System.out.println("Server started");
    }

    public static void stop() throws ServerException {
        try {
            registry.unbind(ServerUtil.BANK_URL);
            UnicastRemoteObject.unexportObject(bank, true);
            UnicastRemoteObject.unexportObject(registry, true);
        } catch (RemoteException | NotBoundException e) {
            throw new ServerException("Can't unbind bank from registry", e);
        }

        System.out.println("Server stopped");
        started.set(false);
    }

    public static void main(String[] args) throws ServerException {
        Server.start();
    }
}
