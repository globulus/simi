package net.globulus.simi.api;

import java.util.List;

public final class SimiException extends RuntimeException implements SimiObject {

    private static final String MESSAGE = "message";

    private final SimiObject originalException;
    private final SimiClass clazz;

    public SimiException(SimiObject originalException, SimiClass clazz, String message) {
        super(message);
        this.originalException = originalException;
        this.clazz = clazz;
    }

    @Override
    public SimiClass getSimiClass() {
        return clazz;
    }

    @Override
    public SimiProperty get(String key, SimiEnvironment environment) {
        if (key.equals(MESSAGE)) {
            return new SimiValue.String(getMessage());
        }
        if (originalException == null) {
            return null;
        }
        return originalException.get(key, environment);
    }

    @Override
    public void set(String key, SimiProperty value, SimiEnvironment environment) {
        throw new AssertionError();
    }

    @Override
    public SimiObject clone(boolean mutable) {
        return this;
    }

    @Override
    public List<SimiValue> keys() {
        return null;
    }

    @Override
    public List<SimiValue> values() {
        return null;
    }

    @Override
    public String toCode(int indentationLevel, boolean ignoreFirst) {
        return null; // TODO revisit
    }

    @Override
    public int getLineNumber() {
        return -1;
    }

    @Override
    public String getFileName() {
        return null;
    }

    @Override
    public boolean hasBreakPoint() {
        return false;
    }

    @Override
    public int compareTo(SimiObject o) {
        return clazz.compareTo(o);
    }
}
