package net.globulus.simi.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public interface BlockInterpreter {

    void executeBlock(SimiBlock block, SimiEnvironment environment, int startAt);
    SimiValue getGlobal(String name);
    SimiEnvironment getEnvironment();
    void raiseException(SimiException e);

    SimiObject newObject(boolean immutable, LinkedHashMap<String, SimiValue> props);
    SimiObject newArray(boolean immutable, ArrayList<SimiValue> props);
    SimiObject newInstance(SimiClass clazz, LinkedHashMap<String, SimiValue> props);
}
