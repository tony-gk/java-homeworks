module homeworks {
    requires java.compiler;

    //for implementor tests
    requires java.sql.rowset;
    requires java.desktop;
    requires java.management;
    requires java.management.rmi;

    exports info.kgeorgiy.java.advanced.implementor;
    exports info.kgeorgiy.java.advanced.student;

    provides info.kgeorgiy.java.advanced.implementor.JarImpler
            with ru.ifmo.rain.gunkin.implementor.Implementor;
    provides info.kgeorgiy.java.advanced.student.AdvancedStudentGroupQuery
            with ru.ifmo.rain.gunkin.student.StudentDB;
}