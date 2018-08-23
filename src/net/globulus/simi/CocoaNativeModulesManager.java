package net.globulus.simi;

import net.globulus.simi.api.SimiObject;
import net.globulus.simi.api.SimiProperty;

import java.util.List;

public class CocoaNativeModulesManager implements NativeModulesManager {

    @Override
    public void load(String path, boolean useApiClassName) {
        // FIXME: Impment in objc
    }

    @Override
    public SimiProperty call(String className, String methodName, SimiObject self, Interpreter interpreter, List<SimiProperty> args) throws IllegalArgumentException {
        // FIXME: Impment in objc
        return null;
    }
}
