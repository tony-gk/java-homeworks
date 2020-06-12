package ru.ifmo.rain.gunkin.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.util.Objects;

public class ServerUtils {
    public static void parseArgsAndStartServer(String[] args, HelloServer server) {
        Objects.requireNonNull(args, "Arguments array is null");
        if (args.length != 2) {
            throw new IllegalArgumentException("Expected 2 arguments");
        }

        for (int i = 0; i < args.length; i++) {
            Objects.requireNonNull(args[i], "Argument " + i + " is null");
        }

        int port = parseArgument(args[0], "port");
        int threadCount = parseArgument(args[0], "count of threads");
        server.start(port, threadCount);
    }

    private static int parseArgument(String arg, String name) {
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Expected integer " + name);
        }
    }
}
