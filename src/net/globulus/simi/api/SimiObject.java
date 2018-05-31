package net.globulus.simi.api;

import java.util.List;

public interface SimiObject extends Codifiable {
    SimiClass getSimiClass();
    SimiProperty get(String key, SimiEnvironment environment);
    void set(String key, SimiProperty prop, SimiEnvironment environment);
    SimiObject clone(boolean mutable);
    List<SimiValue> values();
}
