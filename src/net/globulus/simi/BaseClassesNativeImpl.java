package net.globulus.simi;

import net.globulus.simi.api.*;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.UnaryOperator;
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
        Map<String, BiFunction<Double, Double, Double>> binaries = new HashMap<>();
        binaries.put("pow", Math::pow);
        binaries.put("min", Math::min);
        binaries.put("max", Math::max);
        binaries.put("atan2", Math::atan2);
        binaries.put("hypot", Math::hypot);
        for (Map.Entry<String, BiFunction<Double, Double, Double>> binary : binaries.entrySet()) {
            methods.put(new OverloadableFunction(binary.getKey(), 2), new SimiCallable() {
                @Override
                public int arity() {
                    return 2;
                }

                @Override
                public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                    return doMath(arguments, binary.getValue());
                }
            });
        }
        Map<String, Function<Double, Double>> unaries = new HashMap<>();
        unaries.put("abs", Math::abs);
        unaries.put("acos", Math::acos);
        unaries.put("asin", Math::asin);
        unaries.put("atan", Math::atan);
        unaries.put("cbrt", Math::cbrt);
        unaries.put("ceil", Math::ceil);
        unaries.put("cos", Math::cos);
        unaries.put("cosh", Math::cosh);
        unaries.put("exp", Math::exp);
        unaries.put("floor", Math::floor);
        unaries.put("log", Math::log);
        unaries.put("log10", Math::log10);
        unaries.put("log1p", Math::log1p);
        unaries.put("signum", Math::signum);
        unaries.put("sin", Math::sin);
        unaries.put("sinh", Math::sinh);
        unaries.put("sqrt", Math::sqrt);
        unaries.put("tan", Math::tan);
        unaries.put("tanh", Math::tanh);
        unaries.put("toDegrees", Math::toDegrees);
        unaries.put("toRadians", Math::toRadians);
        for (Map.Entry<String, Function<Double, Double>> unary : unaries.entrySet()) {
            methods.put(new OverloadableFunction(unary.getKey(), 1), new SimiCallable() {
                @Override
                public int arity() {
                    return 1;
                }

                @Override
                public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                    return doMath(arguments, unary.getValue());
                }
            });
        }
        methods.put(new OverloadableFunction("round", 1), new SimiCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                double a = arguments.get(1).getNumber();
                return new SimiValue.Number(Math.round(a));
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

    private SimiValue doMath(List<SimiValue> arguments, BiFunction<Double, Double, Double> op) {
        double a = arguments.get(1).getNumber();
        double b = arguments.get(2).getNumber();
        return new SimiValue.Number(op.apply(a, b));
    }

    private SimiValue doMath(List<SimiValue> arguments, Function<Double, Double> op) {
        double a = arguments.get(1).getNumber();
        return new SimiValue.Number(op.apply(a));
    }

    SimiNativeClass get(String className) {
        return classes.get(className);
    }

    SimiCallable get(String className, String methodName, int arity) {
        return get(className).methods.get(new OverloadableFunction(methodName, arity));
    }
}
