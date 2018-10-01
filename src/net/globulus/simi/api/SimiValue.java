package net.globulus.simi.api;

import java.util.List;

public abstract class SimiValue implements SimiProperty, Codifiable, Comparable<SimiValue> {

    protected SimiValue() { }

    public abstract SimiValue copy();
    public abstract SimiValue clone(boolean mutable);

    @Override
    public SimiValue getValue() {
        return this;
    }

    @Override
    public void setValue(SimiValue value) {
        throw new UnsupportedOperationException("Can't set value of SimiValue!");
    }

    @Override
    public List<SimiObject> getAnnotations() {
        return null;
    }

    @Override
    public int getLineNumber() {
        return -1;
    }

    @Override
    public java.lang.String getFileName() {
        return null;
    }

    @Override
    public boolean hasBreakPoint() {
        return false;
    }

    public java.lang.String getString() {
        if (this instanceof String) {
            return ((String) this).value;
        }
        throw new IncompatibleValuesException(this.getClass(), String.class);
    }

    public Number getNumber() {
        if (this instanceof Number) {
            return (Number) this;
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
            if (!(obj instanceof String)) {
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

        @Override
        public java.lang.String toCode(int indentationLevel, boolean ignoreFirst) {
            return "\"" + toString().replace("\"", "\\\"") + "\"";
        }
    }

    public static class Number extends SimiValue {

        private final Double valueDouble;
        private final Long valueLong;

        public static final Number TRUE = new Number(true);
        public static final Number FALSE = new Number(false);

        public Number(double value) {
            this.valueDouble = value;
            this.valueLong = null;
        }

        public Number(long value) {
            this.valueLong = value;
            this.valueDouble = null;
        }

        public Number(boolean value) {
            this(value ? 1L : 0L);
        }

        public double asDouble() {
            return (valueDouble != null) ? valueDouble : (valueLong * 1.0);
        }

        public long asLong() {
            return (valueLong != null) ? valueLong : Math.round(valueDouble);
        }

        @Override
        public java.lang.String toString() {
            return (valueDouble != null) ? valueDouble.toString() : valueLong.toString();
        }

        @Override
        public boolean equals(java.lang.Object obj) {
            if (!(obj instanceof Number)) {
                return false;
            }
            if (valueLong != null) {
                return Long.compare(valueLong, ((Number) obj).asLong()) == 0;
            }
            return Double.compare(valueDouble, ((Number) obj).asDouble()) == 0;
        }

        @Override
        public SimiValue copy() {
            if (valueLong != null) {
                return new Number(valueLong);
            }
            return new Number(valueDouble);
        }

        @Override
        public SimiValue clone(boolean mutable) {
            return copy();
        }

        @Override
        public int compareTo(SimiValue o) {
            if (!(o instanceof Number)) {
                throw new IncompatibleValuesException(this.getClass(), o.getClass());
            }if (valueLong != null) {
                return Long.compare(valueLong, ((Number) o).asLong());
            }
            return Double.compare(valueDouble, ((Number) o).asDouble());
        }

        @Override
        public java.lang.String toCode(int indentationLevel, boolean ignoreFirst) {
            return toString();
        }

        public Number lessThan(Number o) {
            if (valueLong != null && o.valueLong != null) {
                return new Number(valueLong < o.valueLong);
            }
            return new Number(asDouble() < o.asDouble());
        }

        public Number lessOrEqual(Number o) {
            if (valueLong != null && o.valueLong != null) {
                return new Number(valueLong <= o.valueLong);
            }
            return new Number(asDouble() <= o.asDouble());
        }

        public Number greaterThan(Number o) {
            if (valueLong != null && o.valueLong != null) {
                return new Number(valueLong > o.valueLong);
            }
            return new Number(asDouble() > o.asDouble());
        }

        public Number greaterOrEqual(Number o) {
            if (valueLong != null && o.valueLong != null) {
                return new Number(valueLong >= o.valueLong);
            }
            return new Number(asDouble() >= o.asDouble());
        }

        public Number add(Number o) {
            if (valueLong != null && o.valueLong != null) {
                return new Number(valueLong + o.valueLong);
            }
            return new Number(asDouble() + asDouble());
        }

        public Number subtract(Number o) {
            if (valueLong != null && o.valueLong != null) {
                return new Number(valueLong - o.valueLong);
            }
            return new Number(asDouble() - asDouble());
        }

        public Number multiply(Number o) {
            if (valueLong != null && o.valueLong != null) {
                return new Number(valueLong * o.valueLong);
            }
            return new Number(asDouble() * asDouble());
        }

        public Number divide(Number o) {
            if (valueLong != null && o.valueLong != null) {
                return new Number(valueLong * 1.0 / o.valueLong);
            }
            return new Number(asDouble() / asDouble());
        }

        public Number mod(Number o) {
            if (valueLong != null && o.valueLong != null) {
                return new Number(valueLong % o.valueLong);
            }
            return new Number(asDouble() % asDouble());
        }

        public Number negate() {
            if (valueLong != null) {
                return new Number(-valueLong);
            }
            return new Number(-valueDouble);
        }

        public java.lang.Object getJavaValue() {
            if (valueLong != null) {
                return valueLong;
            }
            return valueDouble;
        }

        public boolean isInteger() {
            return valueLong != null;
        }
    }

    public static class Object extends SimiValue {

        public final SimiObject value;

        public Object(SimiObject value) {
            this.value = value;
        }

        @Override
        public java.lang.String toString() {
            return (value != null) ? value.toString() : "nil";
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
            return (value != null) ? new Object(value.clone(mutable)) : null;
        }

        @Override
        public int compareTo(SimiValue o) {
            throw new RuntimeException("Unable to compare objects by default, implement in subclass!");
        }

        @Override
        public java.lang.String toCode(int indentationLevel, boolean ignoreFirst) {
            return value.toCode(indentationLevel, ignoreFirst);
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

        @Override
        public java.lang.String toCode(int indentationLevel, boolean ignoreFirst) {
            return value.toCode(indentationLevel, ignoreFirst);
        }
    }

    public static class IncompatibleValuesException extends RuntimeException {

        IncompatibleValuesException(Class<? extends SimiValue> value, Class<? extends SimiValue> expected) {
            super("Incompatible types, expected " + expected.getSimpleName() + ", got " + value.getSimpleName());
        }
    }
}
