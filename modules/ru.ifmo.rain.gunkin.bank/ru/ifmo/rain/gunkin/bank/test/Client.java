package ru.ifmo.rain.gunkin.bank.test;

import ru.ifmo.rain.gunkin.bank.main.Account;
import ru.ifmo.rain.gunkin.bank.main.Bank;
import ru.ifmo.rain.gunkin.bank.main.Person;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class Client {
    public static void main(final String... args) throws RemoteException {
        final Bank bank;
        try {
            bank = (Bank) Naming.lookup("//localhost/bank");
        } catch (final NotBoundException e) {
            System.out.println("Bank is not bound");
            return;
        } catch (final MalformedURLException e) {
            System.out.println("Bank URL is invalid");
            return;
        }

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
