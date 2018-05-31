package net.globulus.simi.api;

import java.util.List;

public final class SimiException extends RuntimeException implements SimiObject {

    private final SimiClass clazz;

    public SimiException(SimiClass clazz, String message) {
        super(message);
        this.clazz = clazz;
    }

    @Override
    public SimiClass getSimiClass() {
        return clazz;
    }

    @Override
    public SimiProperty get(String key, SimiEnvironment environment) {
        if (key.equals("message")) {
            return new SimiValue.String(getMessage());
        }
        return null;
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
    public List<SimiValue> values() {
        return null;
    }

    @Override
    public String toCode() {
        return null; // TODO revisit
    }
}
