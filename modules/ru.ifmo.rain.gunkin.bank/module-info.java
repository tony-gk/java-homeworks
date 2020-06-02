module ru.ifmo.rain.gunkin.bank {
    requires java.rmi;
    requires junit;

    exports ru.ifmo.rain.gunkin.bank.server;
    exports ru.ifmo.rain.gunkin.bank.common;
    exports ru.ifmo.rain.gunkin.bank.test;
}