package net.globulus.simi.api;

import java.util.List;

public interface SimiObject {
    SimiClass getSimiClass();
    SimiValue get(String key, SimiEnvironment environment);
    void set(String key, SimiValue value, SimiEnvironment environment);
    SimiObject clone(boolean mutable);
    List<SimiValue> values();
}
