package net.globulus.simi.api;

public interface SimiObject {
    SimiClass getSimiClass();
    SimiValue get(String key, SimiEnvironment environment);
    void set(String key, SimiValue value, SimiEnvironment environment);
}
