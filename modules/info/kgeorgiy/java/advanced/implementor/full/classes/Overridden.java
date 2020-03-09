package info.kgeorgiy.java.advanced.implementor.full.classes;

/**
 * @author Georgiy Korneev (kgeorgiy@kgeorgiy.info)
 */
public class Overridden {
    public static final Class<?>[] FAILED = {PrivateClass.class};

    public static class Base {
        protected int hello() {
            return 0;
        }
    }

    public static abstract class Child extends Base {
        protected abstract int hello();
    }

    public static abstract class GrandChild extends Base {
        protected final int hello() {
            return 0;
        }
    }

    private static abstract class PrivateClass {
        public PrivateClass(int value) {

        }

        public abstract int hello();
    }
}
