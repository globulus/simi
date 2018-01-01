package net.globulus.simi.api;

public class SimiValue {

    private SimiValue() { }

    public java.lang.String getString() {
        if (this instanceof String) {
            return ((String) this).value;
        }
        throw new IncompatibleValuesException(this.getClass(), String.class);
    }

    public Double getNumber() {
        if (this instanceof Number) {
            return ((Number) this).value;
        }
        throw new IncompatibleValuesException(this.getClass(), Number.class);
    }

    public SimiObject getObject() {
        if (this instanceof Object) {
            return ((Object) this).value;
        }
        throw new IncompatibleValuesException(this.getClass(), Object.class);
    }

    public SimiCallable getCallable() {
        if (this instanceof Callable) {
            return ((Callable) this).value;
        }
        throw new IncompatibleValuesException(this.getClass(), Callable.class);
    }

    public static class String extends SimiValue {

        public final java.lang.String value;

        public String(java.lang.String value) {
            this.value = value;
        }
    }

    public static class Number extends SimiValue {

        public final double value;

        public Number(double value) {
            this.value = value;
        }

        public Number(boolean value) {
            this.value = value ? 1 : 0;
        }
    }

    public static class Object extends SimiValue {

        public final SimiObject value;

        public Object(SimiObject value) {
            this.value = value;
        }
    }

    public static class Callable extends SimiValue {

        public final SimiCallable value;

        public Callable(SimiCallable value) {
            this.value = value;
        }
    }

    public static class IncompatibleValuesException extends RuntimeException {

        IncompatibleValuesException(Class<? extends SimiValue> value, Class<? extends SimiValue> expected) {
            super("Incompatible types, expected " + expected.getSimpleName() + ", expected " + value.getSimpleName());
        }
    }
}
