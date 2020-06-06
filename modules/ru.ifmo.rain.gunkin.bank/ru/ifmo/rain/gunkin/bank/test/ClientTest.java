package ru.ifmo.rain.gunkin.bank.test;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import ru.ifmo.rain.gunkin.bank.client.Client;
import ru.ifmo.rain.gunkin.bank.client.ClientException;
import ru.ifmo.rain.gunkin.bank.util.TestUtil;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ClientTest extends BaseTest {

    @Test
    public void test01_testRunClientOnce() throws Exception {
        int amount = 200;
        Client.run(FIRST_NAME, SECOND_NAME, PASSPORT_ID, SUB_ID, amount);

        TestUtil.checkPerson(bank.getRemotePerson(PASSPORT_ID), FIRST_NAME, SECOND_NAME, PASSPORT_ID);
        TestUtil.checkPerson(bank.getLocalPerson(PASSPORT_ID), FIRST_NAME, SECOND_NAME, PASSPORT_ID);
        TestUtil.checkAccountCreated(PASSPORT_ID, SUB_ID, bank, amount);
    }

    @Test
    public void test02_testRunClientMany() throws Exception {
        int amount = 200;
        int times = 10;

        for (int i = 0; i < times; i++) {
            Client.run(FIRST_NAME, SECOND_NAME, PASSPORT_ID, SUB_ID, amount);
        }

        TestUtil.checkAccountCreated(PASSPORT_ID, SUB_ID, bank, amount * times);
    }

    @Test(expected = ClientException.class)
    public void test03_testDifferentFirstName() throws Exception {
        Client.run(FIRST_NAME, SECOND_NAME, PASSPORT_ID, SUB_ID, 0);

        String otherFirstName = FIRST_NAME + "s";
        Client.run(otherFirstName, SECOND_NAME, PASSPORT_ID, SUB_ID, 0);
    }

    @Test(expected = ClientException.class)
    public void test04_testDifferentSecondName() throws Exception {
        Client.run(FIRST_NAME, SECOND_NAME, PASSPORT_ID, SUB_ID, 0);

        String otherSecondName = SECOND_NAME + "s";
        Client.run(FIRST_NAME, otherSecondName, PASSPORT_ID, SUB_ID, 0);
    }
}
