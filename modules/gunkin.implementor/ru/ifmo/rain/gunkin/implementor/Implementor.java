package ru.ifmo.rain.gunkin.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Implementation of the {@code Impler} interface.
 */
public class Implementor implements Impler {
    /**
     * System-dependent line separator string.
     */
    private static final String EOL = System.lineSeparator();

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
        requireNotNull(token, root);

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
     * Writes code implementation of the {@code token} class to the specified {@code writer}.
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
            writer.write(toUnicode(String.format("package %s;%n%n", packageName)));
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
        writer.write(toUnicode(String.format("public class %sImpl %s %s ",
                token.getSimpleName(),
                token.isInterface() ? "implements" : "extends",
                token.getCanonicalName())));
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
            writer.write(toUnicode(buildExecutable(constructor)));
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
            writer.write(toUnicode(buildExecutable(method)));
        }
    }

    /**
     * Returns list of abstract methods to be implemented of specified {@code token}.
     *
     * @param token class whose methods to be returned
     * @return the new list
     */
    private List<Method> getAbstractMethods(Class<?> token) {
        List<MethodSignature> finalDeclaredMethods = Arrays.stream(token.getDeclaredMethods())
                .filter(m -> Modifier.isFinal(m.getModifiers()))
                .map(MethodSignature::new).collect(Collectors.toList());

        List<Method> methods = new ArrayList<>(Arrays.asList(token.getMethods()));
        while (token != null) {
            methods.addAll(Arrays.asList(token.getDeclaredMethods()));
            token = token.getSuperclass();
        }

        return methods.stream()
                .filter(m -> Modifier.isAbstract(m.getModifiers()))
                .map(MethodSignature::new)
                .distinct()
                .filter(m -> !finalDeclaredMethods.contains(m))
                .map(MethodSignature::get)
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
     * {@link #buildParameters(Executable, boolean) buildParameters(executable, true)}.
     *
     * <p> Otherwise method returns:
     * {@code  return [default value]} where <i>default value</i> is the value returned by
     * {@link #buildDefaultValue(Class) buildDefaultValue(executable)}.
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
     * case when token is void.class method returns an empty string.
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
     * followed by the name of the parameter.
     *
     * @param parameter whose representation is to be returned
     * @param withType  determines whether the type of parameters is necessary
     * @return a string representation of the {@code parameter}
     */
    private String buildParameter(Parameter parameter, boolean withType) {
        return (withType ? parameter.getType().getCanonicalName() + " " : "") + parameter.getName();
    }


    /**
     * Converts non-ASCII characters in the given string to unicode escaping.
     *
     * @param s to convert
     * @return converted string
     */
    private String toUnicode(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c >= 128) {
                sb.append(String.format("\\u%04X", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Returns path to file containing implementation of {@code token} with specified {@code extension}
     *
     * @param token     type token to return path for
     * @param root      root directory
     * @param extension file extension
     * @return path to file
     */
    protected Path resolveFilePath(Class<?> token, Path root, String extension) {
        return root.resolve(token.getPackageName().replace(".", File.separator))
                .resolve(token.getSimpleName() + "Impl." + extension);
    }


    /**
     * Checks that specified {@code token} and {@code path} are not {@code null}.
     *
     * @param token reference to check for nullity
     * @param path reference to check for nullity
     * @throws ImplerException if the {@code token} or {@code path} is null
     */
    protected void requireNotNull(Class<?> token, Path path) throws ImplerException {
        if (token == null || path == null) {
            throw new ImplerException("Arguments can not be null");
        }
    }

    /**
     * Wrapper of {@link Method}. It's used to prevent identical implementations of abstract methods.
     */
    private static class MethodSignature {
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
        MethodSignature(Method method) {
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
         * Compares this {@code MethodSignature} against the specified object.  Returns
         * true if the objects are the same.  Two {@code MethodSignature} are the same if
         * they have the same name and parameter types and return type.
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MethodSignature that = (MethodSignature) o;
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
