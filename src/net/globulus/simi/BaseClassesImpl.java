package net.globulus.simi;

import net.globulus.simi.api.BlockInterpreter;
import net.globulus.simi.api.SimiCallable;
import net.globulus.simi.api.SimiObject;
import net.globulus.simi.api.SimiValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class BaseClassesImpl {

    private Map<String, SimiNativeClass> classes;

    BaseClassesImpl() {
        classes = new HashMap<>();
        classes.put(Constants.CLASS_OBJECT, getObjectClass());
    }

    private SimiNativeClass getObjectClass() {
        Map<OverloadableFunction, SimiCallable> methods = new HashMap<>();
        methods.put(new OverloadableFunction("len", 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                SimiObjectImpl self = (SimiObjectImpl) arguments.get(0).getObject();
                return new SimiValue.Number(self.length());
            }
        });
        methods.put(new OverloadableFunction("keys", 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                SimiObjectImpl self = (SimiObjectImpl) arguments.get(0).getObject();
                SimiObjectImpl keys = SimiObjectImpl.fromArray(
                        (SimiClassImpl) interpreter.getGlobal(Constants.CLASS_OBJECT).getObject(),
                        self.fields.keySet().stream().map(SimiValue.String::new).collect(Collectors.toList()));
                return new SimiValue.Object(keys);
            }
        });
        return new SimiNativeClass(Constants.CLASS_OBJECT, methods);
    }

    SimiNativeClass get(String className) {
        return classes.get(className);
    }

    SimiCallable get(String className, String methodName, int arity) {
        return get(className).methods.get(new OverloadableFunction(methodName, arity));
    }
}
