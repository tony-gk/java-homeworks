package ru.ifmo.rain.gunkin.walk;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;


public class FnvRecursiveFileVisitor extends SimpleFileVisitor<Path> {
    private static final int FNV_32_PRIME = 0x01000193;
    private static final int BUFFER_SIZE = 1 << 10;

    private final BufferedWriter writer;

    public FnvRecursiveFileVisitor(BufferedWriter writer) {
        this.writer = writer;
    }

    private FileVisitResult write(int hash, String path) throws IOException {
        writer.write(String.format("%08x %s%n", hash, path));
        return FileVisitResult.CONTINUE;
    }

    public void visitFile(String path) throws IOException {
        try {
            Files.walkFileTree(Paths.get(path), this);
        } catch (InvalidPathException e) {
            write(0, path);
        }
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        int hash = 0x811c9dc5;
        try (InputStream reader = Files.newInputStream(file)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                for (int i = 0; i < read; i++) {
                    hash *= FNV_32_PRIME;
                    hash ^= buffer[i] & 255;
                }
            }
        } catch (IOException e) {
            hash = 0;
        }
        return write(hash, file.toString());
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        return write(0, file.toString());
    }
}
