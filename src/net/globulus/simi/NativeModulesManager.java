package net.globulus.simi;

import net.globulus.simi.api.SimiObject;
import net.globulus.simi.api.SimiProperty;

import java.util.List;

interface NativeModulesManager {

    void load(String path);
    SimiProperty call(String className,
                   String methodName,
                   SimiObject self,
                   Interpreter interpreter,
                   List<SimiProperty> args) throws IllegalArgumentException;
}
