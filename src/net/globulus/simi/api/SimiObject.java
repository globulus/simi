package net.globulus.simi.api;

public interface SimiObject {
    SimiClass getSimiClass();
    Object get(String key);
    void set(String key, Object value);
}
