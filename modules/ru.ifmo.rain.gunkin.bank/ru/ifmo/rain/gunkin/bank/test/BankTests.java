package ru.ifmo.rain.gunkin.bank.test;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import ru.ifmo.rain.gunkin.bank.client.Client;

public class BankTests {
    public static void main(String[] args) {
        Result result = new JUnitCore().run(ClientTest.class, PersonAndAccountTest.class);

        if (!result.wasSuccessful()) {
            for (final Failure failure : result.getFailures()) {
                System.err.println("Test " + failure.getDescription().getMethodName() + " failed: " + failure.getMessage());
                if (failure.getException() != null) {
                    failure.getException().printStackTrace();
                }
            }
            System.exit(1);
        } else {
            System.out.println("============================");
            System.out.println("Ok");
            System.exit(0);
        }
    }
}
