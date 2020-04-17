package ru.ifmo.rain.gunkin.walk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RecursiveWalk {
    private final Path input;
    private final Path output;

    RecursiveWalk(final String inputFileName, final String outputFileName) throws WalkerException {
        this.input = getPath(inputFileName, "Invalid path to input file");
        this.output = getPath(outputFileName, "Invalid path to output file");

        if (this.output.getParent() != null && Files.notExists(this.output.getParent())) {
            try {
                Files.createDirectories(this.output.getParent());
            } catch (IOException e) {
                throw new WalkerException("Error during creating directory: " + e.getMessage(), e);
            } catch (SecurityException e) {
                throw new WalkerException("Creating directory permission denied.", e);
            }
        }
    }

    private Path getPath(String filePath, String errorMessage) throws WalkerException {
        try {
            return Paths.get(filePath);
        } catch (InvalidPathException e) {
            throw new WalkerException(errorMessage, e);
        }
    }

    private void walk() throws WalkerException {
        try (BufferedReader reader = Files.newBufferedReader(input)) {
            try (BufferedWriter writer = Files.newBufferedWriter(output)) {
                FnvRecursiveFileVisitor visitor = new FnvRecursiveFileVisitor(writer);
                try {
                    String filePath;
                    while ((filePath = reader.readLine()) != null) {
                        try {
                            visitor.visitFile(filePath);
                        } catch (IOException e) {
                            throw new WalkerException("Error during writing to output file.", e);
                        }
                    }
                } catch (IOException e) {
                    throw new WalkerException("Error during reading input file.", e);
                }
            } catch (IOException e) {
                throw new WalkerException("Error during opening output file for writing.", e);
            }
        } catch (IOException e) {
            throw new WalkerException("Error during opening input file for reading.", e);
        }
    }

    public static void main(String[] args) {
        try {
            if (args == null || args.length < 2 || args[0] == null || args[1] == null) {
                throw new WalkerException("Excepted 2 arguments");
            }
            new RecursiveWalk(args[0], args[1]).walk();
        } catch (WalkerException e) {
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
        }
    }
}
