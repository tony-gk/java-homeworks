package ru.ifmo.rain.gunkin.bank.client;

import ru.ifmo.rain.gunkin.bank.common.Account;
import ru.ifmo.rain.gunkin.bank.common.Bank;
import ru.ifmo.rain.gunkin.bank.common.Person;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {
    public static void main(final String... args) throws RemoteException {
        final Bank bank;
        try {
            Registry registry = LocateRegistry.getRegistry(8888);
            bank = (Bank) registry.lookup("//localhost/bank");
        } catch (final NotBoundException e) {
            System.out.println("Bank is not bound");
            return;
        } //catch (final MalformedURLException e) {
//            System.out.println("Bank URL is invalid");
//            return;
//        }

        final String subId = args.length >= 1 ? args[0] : "geo";
        final String passportId = args.length >= 2 ? args[1] : "pao";

        Account account = bank.getAccount(subId, passportId);
        if (account == null) {
            System.out.println("Creating account");
            account = bank.createAccount(subId, passportId);
        } else {
            System.out.println("Account already exists");
        }
        System.out.println("Account id: " + account.getId());
        System.out.println("Money: " + account.getAmount());
        System.out.println("Adding money");
        account.setAmount(account.getAmount() + 100);
        System.out.println("Money: " + account.getAmount());

        Person localPerson =  bank.getLocalPerson(passportId);
        Person remotePerson = bank.getRemotePerson(passportId);
        remotePerson.getAccount(subId).setAmount(remotePerson.getAccount(subId).getAmount() + 1);
        System.out.println("Remote person updated amount of " + subId + ": " + account.getAmount());

        System.out.println("View of local person " + localPerson.getAccount(subId).getAmount());

    }
}
