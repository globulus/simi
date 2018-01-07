package net.globulus.simi;

import net.globulus.simi.api.*;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.*;
import java.util.stream.Collectors;

class BaseClassesNativeImpl {

    private Map<String, SimiNativeClass> classes;

    BaseClassesNativeImpl() {
        classes = new HashMap<>();
        classes.put(Constants.CLASS_OBJECT, getObjectClass());
        classes.put(Constants.CLASS_STRING, getStringClass());
        classes.put(Constants.CLASS_GLOBALS, getGlobalsClass());
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
                return 1;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                SimiObjectImpl self = (SimiObjectImpl) arguments.get(0).getObject();
                return new SimiValue.Number(self.contains(arguments.get(1), Token.nativeCall(Constants.HAS)));
            }
        });
        return new SimiNativeClass(Constants.CLASS_OBJECT, methods);
    }

    private SimiNativeClass getStringClass() {
        Map<OverloadableFunction, SimiCallable> methods = new HashMap<>();
        methods.put(new OverloadableFunction("len", 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                String value = prepareStringNativeCall(interpreter, arguments);
                return new SimiValue.Number(value.length());
            }
        });
        methods.put(new OverloadableFunction(Constants.ITERATE, 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                String value = prepareStringNativeCall(interpreter, arguments);
                StringCharacterIterator iterator = new StringCharacterIterator(value);
                LinkedHashMap<String, SimiValue> fields = new LinkedHashMap<>();
                fields.put(Constants.NEXT, new SimiValue.Callable(new SimiCallable() {
                    @Override
                    public int arity() {
                        return 0;
                    }

                    @Override
                    public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                       char next = iterator.next();
                       if (next != CharacterIterator.DONE) {
                           return new SimiValue.String("" + next);
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
                return 1;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                String value = prepareStringNativeCall(interpreter, arguments);
                String str = arguments.get(1).getString();
                return new SimiValue.Number(value.contains(str));
            }
        });
        return new SimiNativeClass(Constants.CLASS_OBJECT, methods);
    }

    private SimiNativeClass getGlobalsClass() {
        Map<OverloadableFunction, SimiCallable> methods = new HashMap<>();
        methods.put(new OverloadableFunction("pow", 2), new SimiCallable() {
            @Override
            public int arity() {
                return 2;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                double a = arguments.get(1).getNumber();
                double b = arguments.get(2).getNumber();
                return new SimiValue.Number(Math.pow(a, b));
            }
        });
        return new SimiNativeClass(Constants.CLASS_GLOBALS, methods);
    }

    private String prepareStringNativeCall(BlockInterpreter interpreter, List<SimiValue> arguments) {
        SimiValue arg0 = arguments.get(0);
        SimiObjectImpl self = (SimiObjectImpl) arg0.getObject();
        SimiEnvironment environment = interpreter.getEnvironment();
        environment.define(Constants.SELF, arg0);
        return self.get(Constants.PRIVATE, environment).getString();
    }

    SimiNativeClass get(String className) {
        return classes.get(className);
    }

    SimiCallable get(String className, String methodName, int arity) {
        return get(className).methods.get(new OverloadableFunction(methodName, arity));
    }
}
