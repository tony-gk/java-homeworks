package ru.ifmo.rain.gunkin.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.nio.file.Paths;

public class Test {
    public static void main(String[] args) throws ImplerException {
        Implementor implementor = new Implementor();
        implementor.implementJar(Exception.class, Paths.get("/home/tony/study/java/lessons/avot.jar"));

    }
}
