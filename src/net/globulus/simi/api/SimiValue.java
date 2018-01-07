package net.globulus.simi.api;

public abstract class SimiValue {

    protected SimiValue() { }

    public abstract SimiValue copy();

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

        @Override
        public java.lang.String toString() {
            return value;
        }

        @Override
        public boolean equals(java.lang.Object obj) {
            if (obj == null || !(obj instanceof SimiValue.String)) {
                return false;
            }
            return value.equals(((String) obj).value);
        }

        @Override
        public SimiValue copy() {
            return new String(value);
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

        @Override
        public java.lang.String toString() {
            java.lang.String text = "" + value;
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        @Override
        public boolean equals(java.lang.Object obj) {
            if (obj == null || !(obj instanceof SimiValue.Number)) {
                return false;
            }
            return value == ((Number) obj).value;
        }

        @Override
        public SimiValue copy() {
            return new Number(value);
        }
    }

    public static class Object extends SimiValue {

        public final SimiObject value;

        public Object(SimiObject value) {
            this.value = value;
        }

        @Override
        public java.lang.String toString() {
            return value.toString();
        }

        @Override
        public SimiValue copy() {
            return new Object(value);
        }
    }

    public static class Callable extends SimiValue {

        public final SimiCallable value;
        public final java.lang.String name;
        private SimiObject instance;

        public Callable(SimiCallable value, java.lang.String name, SimiObject instance) {
            this.value = value;
            this.name = name;
            this.instance = instance;
        }

        @Override
        public java.lang.String toString() {
            return value.toString();
        }

        @Override
        public SimiValue copy() {
            return new Callable(value, name, instance);
        }

        public SimiObject getInstance() {
            return instance;
        }

        public void bind(SimiObject instance) {
            this.instance = instance;
        }
    }

    public static class IncompatibleValuesException extends RuntimeException {

        IncompatibleValuesException(Class<? extends SimiValue> value, Class<? extends SimiValue> expected) {
            super("Incompatible types, expected " + expected.getSimpleName() + ", got " + value.getSimpleName());
        }
    }
}
