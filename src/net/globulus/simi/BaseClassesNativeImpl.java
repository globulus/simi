package net.globulus.simi;

import com.sun.tools.internal.jxc.ap.Const;
import net.globulus.simi.api.BlockInterpreter;
import net.globulus.simi.api.SimiCallable;
import net.globulus.simi.api.SimiObject;
import net.globulus.simi.api.SimiValue;

import java.util.*;
import java.util.stream.Collectors;

class BaseClassesNativeImpl {

    private Map<String, SimiNativeClass> classes;

    BaseClassesNativeImpl() {
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
        methods.put(new OverloadableFunction("values", 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                SimiObjectImpl self = (SimiObjectImpl) arguments.get(0).getObject();
                SimiObjectImpl keys = SimiObjectImpl.fromArray(
                        (SimiClassImpl) interpreter.getGlobal(Constants.CLASS_OBJECT).getObject(),
                        new ArrayList<>(self.fields.values()));
                return new SimiValue.Object(keys);
            }
        });
        methods.put(new OverloadableFunction("isMutable", 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                return new SimiValue.Number(!((SimiObjectImpl) arguments.get(0).getObject()).immutable);
            }
        });
        methods.put(new OverloadableFunction("isArray", 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                return new SimiValue.Number(((SimiObjectImpl) arguments.get(0).getObject()).isArray());
            }
        });
        methods.put(new OverloadableFunction(Constants.ITERATE, 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                SimiObjectImpl self = (SimiObjectImpl) arguments.get(0).getObject();
                final boolean isArray = self.isArray();
                final Iterator<?> iterator;
                if (isArray) {
                    iterator = self.fields.values().iterator();
                } else {
                    iterator = self.fields.keySet().iterator();
                }
                LinkedHashMap<String, SimiValue> fields = new LinkedHashMap<>();
                fields.put(Constants.NEXT, new SimiValue.Callable(new SimiCallable() {
                    @Override
                    public int arity() {
                        return 0;
                    }

                    @Override
                    public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                        if (iterator.hasNext()) {
                            if (isArray) {
                                return (SimiValue) iterator.next();
                            } else {
                                return new SimiValue.String((String) iterator.next());
                            }
                        }
                        return null;
                    }
                }, null, null));
                return new SimiValue.Object(new SimiNativeObject(fields));
            }
        });
        methods.put(new OverloadableFunction(Constants.HAS, 1), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                SimiObjectImpl self = (SimiObjectImpl) arguments.get(0).getObject();
                return new SimiValue.Number(self.contains(arguments.get(1), Token.nativeCall(Constants.HAS)));
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
