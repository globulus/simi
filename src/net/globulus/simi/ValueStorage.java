package net.globulus.simi;

import net.globulus.simi.api.SimiCallable;
import net.globulus.simi.api.SimiObject;
import net.globulus.simi.api.SimiValue;

interface ValueStorage {
    void put(String name, Double value);
    void put(String name, String value);
    void put(String name, SimiObject value);
    void put(String name, SimiCallable value);
    void put(String name, SimiValue value);
    void put(String name, Object value);

    Double getNumber(String name);
    String getString(String name);
    SimiObject getObject(String name);
    SimiCallable getCallable(String name);
    SimiValue getValue(String name);
    Object get(String name);

    boolean remove(String name);
}
