package net.globulus.simi.api;

import java.util.List;

public interface SimiEnvironment {
    void define(String name, SimiValue value, List<SimiObject> annotations);
    SimiProperty tryGet(String name);
}
