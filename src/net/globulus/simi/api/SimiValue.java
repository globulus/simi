package net.globulus.simi.api;

public abstract class SimiValue implements Comparable<SimiValue> {

    protected SimiValue() { }

    public abstract SimiValue copy();
    public abstract SimiValue clone(boolean mutable);

    public static SimiValue pack(java.lang.Object value, java.lang.Object... args) {
        if (value instanceof Double) {
            return new Number((Double) value);
        }
        if (value instanceof java.lang.String) {
            return new String((java.lang.String) value);
        }
        if (value instanceof SimiObject) {
            return new Object((SimiObject) value);
        }
        if (value instanceof SimiCallable) {
            return new Callable((SimiCallable) value, (java.lang.String) args[0], (SimiObject) args[1]);
        }
        throw new IllegalArgumentException("Trying to pack " + value.toString() + " to SimiValue.");
    }

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

        @Override
        public SimiValue clone(boolean mutable) {
            return copy();
        }

        @Override
        public int compareTo(SimiValue o) {
            if (!(o instanceof String)) {
                throw new IncompatibleValuesException(this.getClass(), o.getClass());
            }
            return this.value.compareTo(((String) o).value);
        }
    }

    public static class Number extends SimiValue {

        public final double value;

        public static final Number TRUE = new Number(true);
        public static final Number FALSE = new Number(false);

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
            return Double.compare(value, ((Number) obj).value) == 0;
        }

        @Override
        public SimiValue copy() {
            return new Number(value);
        }

        @Override
        public SimiValue clone(boolean mutable) {
            return copy();
        }

        @Override
        public int compareTo(SimiValue o) {
            if (!(o instanceof Number)) {
                throw new IncompatibleValuesException(this.getClass(), o.getClass());
            }
            return Double.compare(this.value, ((Number) o).value);
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
        public boolean equals(java.lang.Object obj) {
            return this == obj;
        }

        @Override
        public SimiValue copy() {
            return new Object(value);
        }

        @Override
        public SimiValue clone(boolean mutable) {
            return new Object(value.clone(mutable));
        }

        @Override
        public int compareTo(SimiValue o) {
            throw new RuntimeException("Unable to compare objects by default, implement in subclass!");
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
        public boolean equals(java.lang.Object obj) {
            return this == obj;
        }

        @Override
        public SimiValue copy() {
            return new Callable(value, name, instance);
        }

        @Override
        public SimiValue clone(boolean mutable) {
            return copy();
        }

        public SimiObject getInstance() {
            return instance;
        }

        public void bind(SimiObject instance) {
            this.instance = instance;
        }

        @Override
        public int compareTo(SimiValue o) {
            throw new RuntimeException("Unable to compare callables!");
        }
    }

    public static class IncompatibleValuesException extends RuntimeException {

        IncompatibleValuesException(Class<? extends SimiValue> value, Class<? extends SimiValue> expected) {
            super("Incompatible types, expected " + expected.getSimpleName() + ", got " + value.getSimpleName());
        }
    }
}
