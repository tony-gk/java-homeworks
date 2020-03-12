package ru.ifmo.rain.gunkin.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

public class Implementor implements JarImpler {
    /**
     * System-dependent line separator string.
     */
    private static final String EOL = System.lineSeparator();

    /**
     * Entry point into the application.
     *
     * @param args Usage:
     *             <ul>
     *             <li> {@code <classToken> <rootPath>} to produce file implementing {@code classToken} in {@code rootPath}</li>
     *             <li>{@code -jar <classToken> <jarPath>} to produce jar file in {@code jarPath} implementing {@code classToken}</li>
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

        Implementor implementor = new Implementor();

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
     * Produces code implementing class or interface specified by provided {@code token}.
     * <p>
     * Generated class classes name should be same as classes name of the type token with {@code Impl} suffix
     * added. Generated source code should be placed in the correct subdirectory of the specified
     * {@code root} directory and have correct file name. For example, the implementation of the
     * interface {@link List} should go to {@code $root/java/util/ListImpl.java}
     *
     * @param token type token to create implementation for.
     * @param root  root directory.
     * @throws ImplerException when implementation cannot be generated for one of such reasons:
     *                         <ul>
     *                         <li> one of arguments is null</li>
     *                         <li> {@code token} is primitive or array</li>
     *                         <li> {@code token} is final or private</li>
     *                         <li> {@code token} is {@link Enum}</li>
     *                         <li> Error occurred during writing or creating files</li>
     *                         </ul>
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (token == null || root == null) {
            throw new ImplerException("Arguments must be not-null");
        }

        if (token.isPrimitive() || token.isArray() || Modifier.isFinal(token.getModifiers()) || Modifier.isPrivate(token.getModifiers()) || token == Enum.class) {
            throw new ImplerException("Invalid class token");
        }

        Path filePath = resolveFilePath(token, root, "java");

        try {
            Files.createDirectories(filePath.getParent());
        } catch (IOException | SecurityException e) {
            throw new ImplerException("Failed to create directories", e);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
            implement(token, writer);
        } catch (IOException e) {
            throw new ImplerException("Can't write to " + filePath.toString(), e);
        }
    }

    /**
     * Returns path to file containing implementation of {@code token} with specified {@code extension}
     *
     * @param token     type token to return path for
     * @param root      root directory
     * @param extension file extension
     * @return path to file
     */
    private Path resolveFilePath(Class<?> token, Path root, String extension) {
        return root.resolve(token.getPackageName().replace(".", File.separator))
                .resolve(token.getSimpleName() + "Impl." + extension);
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
        if (token == null || jarFile == null) {
            throw new ImplerException("Arguments can not be null");
        }

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
            deleteDirectoryOnExit(tempDir);
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
     * Writes code implementation of the {@code token} class to specified {@code writer}.
     *
     * @param token  class the header of which is to be written
     * @param writer to write to
     * @throws IOException     if an I/O error thrown during writing
     * @throws ImplerException if provided {@code token} is not an interface and it doesn't have any public or protected constructors
     */
    private void implement(Class<?> token, Writer writer) throws IOException, ImplerException {
        writePackage(token, writer);
        writeClassHeader(token, writer);

        writer.write("{" + EOL);

        if (!token.isInterface()) {
            writeConstructors(token, writer);
        }
        writeMethods(token, writer);

        writer.write("}" + EOL);
    }

    /**
     * Writes the package of the {@code token} class to the specified {@code writer}.
     *
     * @param token  class the package of which is to be written
     * @param writer to write to
     * @throws IOException if an I/O error thrown during writing
     */
    private void writePackage(Class<?> token, Writer writer) throws IOException {
        String packageName = token.getPackageName();
        if (!packageName.equals("")) {
            writer.write(String.format("package %s;%n%n", packageName));
        }
    }

    /**
     * Writes the header of the {@code token} class declaration to the specified {@code writer}.
     *
     * @param token  class the header of which is to be written
     * @param writer to write to
     * @throws IOException if an I/O error thrown during writing
     */
    private void writeClassHeader(Class<?> token, Writer writer) throws IOException {
        writer.write(String.format("public class %sImpl %s %s ",
                token.getSimpleName(),
                token.isInterface() ? "implements" : "extends",
                token.getCanonicalName()));
    }

    /**
     * Writes built constructors of {@code token} class to specified {@code writer}.
     *
     * @param token  class whose constructors are to be written
     * @param writer to write to
     * @throws IOException     if an I/O error thrown during writing
     * @throws ImplerException if {@code token} doesn't have any public or protected constructors
     */
    private void writeConstructors(Class<?> token, Writer writer) throws IOException, ImplerException {
        List<Constructor<?>> constructors = Arrays.stream(token.getDeclaredConstructors())
                .filter(c -> !Modifier.isPrivate(c.getModifiers()))
                .collect(Collectors.toList());
        if (constructors.isEmpty()) {
            throw new ImplerException("No public or protected constructors in class");
        }
        for (Constructor<?> constructor : token.getDeclaredConstructors()) {
            writer.write(buildExecutable(constructor));
        }
    }

    /**
     * Writes built abstract methods of {@code token} class to specified {@code writer}.
     *
     * @param token  class whose methods are to be written
     * @param writer to write to
     * @throws IOException if an I/O error thrown during writing
     */
    private void writeMethods(Class<?> token, Writer writer) throws IOException {
        for (Method method : getAbstractMethods(token)) {
            writer.write(buildExecutable(method));
        }
    }

    /**
     * Returns list of abstract methods to be implemented of specified {@code token}.
     *
     * @param token class whose methods to be returned
     * @return the new list
     */
    private List<Method> getAbstractMethods(Class<?> token) {
        List<MethodWrapper> finalDeclaredMethods = Arrays.stream(token.getDeclaredMethods())
                .filter(m -> Modifier.isFinal(m.getModifiers()))
                .map(MethodWrapper::new).collect(Collectors.toList());

        List<Method> methods = new ArrayList<>(Arrays.asList(token.getMethods()));
        while (token != null) {
            methods.addAll(Arrays.asList(token.getDeclaredMethods()));
            token = token.getSuperclass();
        }

        return methods.stream()
                .filter(m -> Modifier.isAbstract(m.getModifiers()))
                .map(MethodWrapper::new)
                .distinct()
                .filter(m -> !finalDeclaredMethods.contains(m))
                .map(MethodWrapper::get)
                .collect(Collectors.toList());
    }

    /**
     * Returns java code as a {@link String} that representing compilable
     * {@link Method} or {@link Constructor}.
     *
     * @param executable object which representation is to be returned
     * @return {@link String} representation of {@code executable}
     */
    private String buildExecutable(Executable executable) {
        return "\tpublic " + buildReturnTypeAndName(executable) +
                "(" + buildParameters(executable, true) + ")" +
                buildExceptions(executable) + " {" + EOL +
                "\t\t" + buildBody(executable) + ";" + EOL +
                "\t}" + EOL;
    }

    /**
     * Returns return type in canonical form (if {@code executable} is instance of {@link Method})
     * and name of {@code executable} object.
     *
     * @param executable object which return type and name is to be returned
     * @return {@link String} representation of return type (if needed) and name of
     * {@code executable} object
     */

    private String buildReturnTypeAndName(Executable executable) {
        if (executable instanceof Method) {
            return ((Method) executable).getReturnType().getCanonicalName() + " " + executable.getName();
        } else {
            return executable.getDeclaringClass().getSimpleName() + "Impl";
        }
    }

    /**
     * Returns list of exceptions, that specified {@code executable} throws. If {@code executable}
     * doesn't throw any exceptions returns an empty string. Otherwise the format is
     * the word {@code throws}, followed by a comma-separated list of the thrown exception types
     * in canonical form.
     *
     * @param executable object whose exceptions is to be returned
     * @return string representation of exceptions thrown by {@code executable} if any,
     * otherwise an empty string
     */
    private String buildExceptions(Executable executable) {
        Class<?>[] exceptions = executable.getExceptionTypes();
        if (exceptions.length > 0) {
            return " throws " + Arrays.stream(exceptions)
                    .map(Class::getCanonicalName)
                    .collect(Collectors.joining(", "));
        }
        return "";
    }

    /**
     * Returns string representing body of the {@code executable} object.
     *
     * <p> In the case when the @{code executable} is instance of {@link Constructor} the format is:
     * {@code super([args])} where <i>args</i> is the result of
     * {@link #buildParameters(Executable, boolean) buildParameters(executable, true)}
     *
     * <p> Otherwise method returns:
     * {@code  return [default value]} where <i>default value</i> is the value returned by
     * {@link #buildDefaultValue(Class) buildDefaultValue(executable)}
     *
     * @param executable {@link Constructor} or {@link Method} object.
     * @return string representation of the body of the executable object
     */
    private String buildBody(Executable executable) {
        if (executable instanceof Constructor) {
            return "super(" + buildParameters(executable, false) + ")";
        } else {
            return "return" + buildDefaultValue(((Method) executable).getReturnType());
        }
    }

    /**
     * Returns default value of specified {@code token}. In the
     * case when token is void.class method returns an empty string
     *
     * @param token whose default value is to be returned
     * @return string representation of the default value
     */
    private String buildDefaultValue(Class<?> token) {
        if (token == boolean.class) {
            return " false";
        } else if (token == void.class) {
            return "";
        } else if (token.isPrimitive()) {
            return " 0";
        }
        return " null";
    }

    /**
     * Returns a string describing parameters of {@code executable}. The format is
     * comma-separated list of the {@code executable}'s parameters names with types
     * (if {@code withType} is true) in canonical form.
     *
     * @param executable object whose parameters are to be returned
     * @param withType   determines whether the type of parameters is necessary
     * @return list of parameters with names and types if needed
     */
    private String buildParameters(Executable executable, boolean withType) {
        return Arrays.stream(executable.getParameters())
                .map(p -> buildParameter(p, withType))
                .collect(Collectors.joining(", "));
    }

    /**
     * Returns a string describing this {@code parameter}. The format is the
     * type (if {@code withType} is true} in canonical name, followed by a space,
     * followed by the name of the parameter
     *
     * @param parameter whose representation is to be returned
     * @param withType  determines whether the type of parameters is necessary
     * @return a string representation of the {@code parameter}
     */
    private String buildParameter(Parameter parameter, boolean withType) {
        return (withType ? parameter.getType().getCanonicalName() + " " : "") + parameter.getName();
    }

    /**
     * Requests that the {@code directory} and all including files
     * be deleted when the virtual machine terminates.
     * Deletion will be attempted only for normal termination of the
     * virtual machine, as defined by the Java Language Specification.
     *
     * @param directory to be deleted
     * @throws ImplerException if an error is thrown when accessing the starting file.
     * @see File#deleteOnExit()
     */
    private void deleteDirectoryOnExit(Path directory) throws ImplerException {
        try {
            Files.walk(directory).map(Path::toFile).forEach(File::deleteOnExit);
        } catch (IOException | SecurityException e) {
            throw new ImplerException("Failed to delete temporary files", e);
        }
    }

    /**
     * Wrapper of {@link Method}. It's used to prevent identical implementations of abstract methods.
     */
    private static class MethodWrapper {
        /**
         * Initial value used for calculating hash.
         */
        private static final int SEED = 23;
        /**
         * Prime number used for calculating hash.
         */
        private static final int PRIME_NUMBER = 37;

        /**
         * Instance wrapped by class.
         */
        private final Method method;

        /**
         * Construct wrapper via provided instance of {@link Method}.
         *
         * @param method object to be wrapped
         */
        MethodWrapper(Method method) {
            this.method = method;
        }

        /**
         * Returns {@link #method}
         *
         * @return wrapped instance
         */
        public Method get() {
            return this.method;
        }

        /**
         * Compares this {@code MethodWrapper} against the specified object.  Returns
         * true if the objects are the same.  Two {@code MethodWrappers} are the same if
         * they have the same name and parameter types and return type.
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MethodWrapper that = (MethodWrapper) o;
            return Objects.equals(method.getReturnType(), that.method.getReturnType())
                    && Arrays.equals(method.getParameterTypes(), that.method.getParameterTypes())
                    && Objects.equals(method.getName(), that.method.getName());
        }

        /**
         * Returns a hashcode for this {@code MethodWrapper}. The hashcode is computed
         * using method's return type, parameter types and name.
         */
        @Override
        public int hashCode() {
            int hash = SEED;
            hash = PRIME_NUMBER * hash + Objects.hashCode(method.getReturnType());
            hash = PRIME_NUMBER * hash + Arrays.hashCode(method.getParameterTypes());
            hash = PRIME_NUMBER * hash + Objects.hashCode(method.getName());
            return hash;
        }
    }
}
