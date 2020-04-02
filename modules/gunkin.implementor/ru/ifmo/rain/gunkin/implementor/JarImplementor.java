package ru.ifmo.rain.gunkin.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * Implementation of the {@code JarImpler} interface.
 */
public class JarImplementor extends Implementor implements JarImpler {

    /**
     * Entry point into the application.
     *
     * @param args Usage:
     *             <ul>
     *             <li> {@code <classToken> <rootPath>} to produce file implementing {@code classToken} in {@code rootPath}</li>
     *             <li> {@code -jar <classToken> <jarPath>} to produce jar file in {@code jarPath} implementing {@code classToken}</li>
     *             </ul>
     */
    static public void main(String[] args) {
        if (args == null || args.length < 2 || args.length > 3) {
            System.err.println("Excepted two or three arguments");
            return;
        }

        if (Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("One of arguments is null");
        }

        JarImpler implementor = new JarImplementor();

        try {
            if (args.length == 2) {
                implementor.implement(Class.forName(args[0]), Paths.get(args[1]));
            } else {
                implementor.implementJar(Class.forName(args[1]), Paths.get(args[2]));
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Invalid class name: " + e.getMessage());
        } catch (ImplerException e) {
            System.err.println(e.getMessage());
        } catch (InvalidPathException e) {
            System.err.println("Invalid root path: " + e.getMessage());
        }

    }


    /**
     * Produces {@code .jar} file implementing class or interface specified by provided {@code token}.
     * <p>
     * Generated class classes name should be same as classes name of the type token with {@code Impl} suffix
     * added.
     *
     * @param token   type token to create implementation for.
     * @param jarFile target {@code .jar} file.
     * @throws ImplerException when implementation cannot be generated for one of such reasons:
     *                         <ul>
     *                         <li> one of arguments is null</li>
     *                         <li> {@code token} is primitive or array</li>
     *                         <li> {@code token} is final or private</li>
     *                         <li> {@code token} is {@link Enum}</li>
     *                         <li> Error occurred during writing, creating files, or compiling</li>
     *                         </ul>
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        requireNotNull(token, jarFile);

        Path tempDir;
        try {
            tempDir = Files.createTempDirectory(jarFile.toAbsolutePath().getParent(), "tmp");
        } catch (IOException e) {
            throw new ImplerException("Failed to create temporary directory", e);
        }

        try {
            implement(token, tempDir);
            compileImplementedFile(token, tempDir);
            createJarFile(token, tempDir, jarFile);
        } finally {
            deleteDirectory(tempDir);
        }
    }

    /**
     * Produces {@code .jar} file implementing class or interface specified by provided {@code token}.
     *
     * @param token   type token to create implementation for
     * @param tempDir directory where {@code token} implementation was compiled via
     *                {@link #compileImplementedFile(Class, Path) compileImplementedFile(token, tempDir}
     * @param jarFile target {@code .jar} file
     * @throws ImplerException if an I/O error thrown during writing to jar file
     */
    private void createJarFile(Class<?> token, Path tempDir, Path jarFile) throws ImplerException {
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (JarOutputStream writer = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            writer.putNextEntry(new ZipEntry((token.getPackageName() + "." + token.getSimpleName()).replace('.', '/') + "Impl.class"));
            Files.copy(resolveFilePath(token, tempDir, "class"), writer);
        } catch (IOException e) {
            throw new ImplerException("Failed to write to JAR file", e);
        }
    }

    /**
     * Compiles java file which was implemented via {@link #implement(Class, Path) implement(token, root)}.
     *
     * @param token class which was implemented
     * @param root  path where class was implemented
     * @throws ImplerException if an error occurred during compiling
     */
    private void compileImplementedFile(Class<?> token, Path root) throws ImplerException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        String[] args = new String[]{
                "-cp",
                root.toString() + File.pathSeparator + System.getProperty("java.class.path"),
                resolveFilePath(token, root, "java").toString()};
        if (compiler == null || compiler.run(null, null, null, args) != 0) {
            throw new ImplerException("Failed to compile implemented file");
        }
    }

    /**
     * Deletes the specified directory and all its contents.
     *
     * @param dir the location of the directory to be deleted
     * @throws ImplerException if an error occurs during directory deletion
     */
    private void deleteDirectory(Path dir) throws ImplerException {
        if (dir == null) {
            return;
        }
        dir = dir.toAbsolutePath();
        try {
            Files.walkFileTree(dir, new DeletionFileVisitor());
        } catch (IOException e) {
            throw new ImplerException("Failed to delete temporary directory: " + dir);
        }
    }

    /**
     * An implementation of the {@code FileVisitor} interface. Used to deleting
     * file trees.
     */
    private static class DeletionFileVisitor extends SimpleFileVisitor<Path> {
        /**
         * Default constructor.
         */
        private DeletionFileVisitor() {
        }

        /**
         * File visitor method. Deletes the specified {@code file}.
         *
         * @param file  a reference to the file
         * @param attrs the file's basic attributes
         * @throws IOException if an I/O error occurs
         */
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        /**
         * Directory visitor method. Deletes the specified {@code dir}.
         *
         * @param dir a reference to the directory
         * @param exc null if the iteration of the directory completes without an error;
         *            otherwise the I/O exception that caused the iteration of the directory to complete prematurely
         * @throws IOException if an I/O error occurs
         */
        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            if (exc == null) {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            } else {
                throw exc;
            }
        }
    }
}
