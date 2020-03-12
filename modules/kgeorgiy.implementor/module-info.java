module kgeorgiy.implementor {
    requires java.management;
    requires java.desktop;
    requires java.management.rmi;
    requires java.sql;
    requires java.sql.rowset;
    exports info.kgeorgiy.java.advanced.implementor;

    opens info.kgeorgiy.java.advanced.implementor.basic.interfaces;
    opens info.kgeorgiy.java.advanced.implementor.basic.interfaces.standard;
    opens info.kgeorgiy.java.advanced.implementor.basic.classes;
    opens info.kgeorgiy.java.advanced.implementor.basic.classes.standard;

    opens info.kgeorgiy.java.advanced.implementor.full.interfaces;
    opens info.kgeorgiy.java.advanced.implementor.full.interfaces.standard;
    opens info.kgeorgiy.java.advanced.implementor.full.classes;
    opens info.kgeorgiy.java.advanced.implementor.full.classes.standard;
    opens info.kgeorgiy.java.advanced.implementor.full.lang;
    exports info.kgeorgiy.java.advanced.implementor.full.lang;
}