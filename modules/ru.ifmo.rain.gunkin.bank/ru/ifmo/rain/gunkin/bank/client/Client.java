package ru.ifmo.rain.gunkin.bank.client;

import ru.ifmo.rain.gunkin.bank.common.Account;
import ru.ifmo.rain.gunkin.bank.common.Bank;
import ru.ifmo.rain.gunkin.bank.common.Person;
import ru.ifmo.rain.gunkin.bank.util.ServerUtil;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Objects;

public class Client {

    public static void run(String firstName, String secondName,
                           String passportId, String subId, int amount) throws RemoteException, NotBoundException {

        Registry registry = LocateRegistry.getRegistry(ServerUtil.PORT);
        Bank bank = (Bank) registry.lookup(ServerUtil.BANK_URL);

        Person person = bank.getRemotePerson(passportId);
        if (person == null) {
            person = bank.registerPerson(firstName, secondName, passportId);
            System.out.println("New person registered");
        } else {
            if (!firstName.equals(person.getFirstName())) {
                System.out.println("Specified first name doesn't match " +
                        "the first name of the person registered with specified passport id");
                return;
            }

            if (!secondName.equals(person.getSecondName())) {
                System.out.println("Specified second name doesn't match " +
                        "the second name of the person registered with specified passport id");
                return;
            }
        }

        Account account = person.getAccount(subId);
        if (account == null) {
            account = person.createAccount(subId);
            System.out.println("New account created");
        }

        account.addAmount(amount);
        System.out.println("First name: " + firstName);
        System.out.println("Second name: " + secondName);
        System.out.println("Passport: " + passportId);
        System.out.println("Account: " + subId);
        System.out.println("Account balance: " + account.getAmount());
    }

    public static void main(String[] args) throws RemoteException, NotBoundException {
        Objects.requireNonNull(args, "Arguments array is null");
        if (args.length != 5) {
            throw new IllegalArgumentException("Expected 5 arguments");
        }

        for (int i = 0; i < args.length; i++) {
            Objects.requireNonNull(args[i], "Argument " + i + " is null");
        }

        String firstName = args[0];
        String secondName = args[1];
        String passportId = args[2];
        String subId = args[3];
        int amount;

        try {
            amount = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Expected integer amount, but was " + args[4], e);
        }

        Client.run(firstName, secondName, passportId, subId, amount);
    }
}
