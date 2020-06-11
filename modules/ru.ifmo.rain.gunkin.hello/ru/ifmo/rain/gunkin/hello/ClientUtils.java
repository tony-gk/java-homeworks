package ru.ifmo.rain.gunkin.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.util.Objects;

public class ClientUtils {
    public static void parseArgsAndRunClient(String[] args, HelloClient client) {
        Objects.requireNonNull(args, "Arguments array is null");
        if (args.length != 5) {
            throw new IllegalArgumentException("Expected 5 arguments");
        }

        for (int i = 0; i < args.length; i++) {
            Objects.requireNonNull(args[i], "Argument " + i + " is null");
        }

        String host = args[0];
        int port = parseArgument(args[1], "port");
        String prefix = args[2];
        int threadCount = parseArgument(args[3], "count of threads");
        int requestCount = parseArgument(args[4], "count of requests");

        client.run(host, port, prefix, threadCount, requestCount);
    }

    private static int parseArgument(String arg, String name) {
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Expected integer " + name);
        }
    }

    public static boolean isValidEvilResponse(String response, int threadNumber, int requestNumber) {
        return response.matches("[\\D]*" + threadNumber + "[\\D]*" + requestNumber + "[\\D]*");
    }
}
