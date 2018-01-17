package net.globulus.simi;

import net.globulus.simi.api.*;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class BaseClassesNativeImpl {

    private Map<String, SimiNativeClass> classes;

    BaseClassesNativeImpl() {
        classes = new HashMap<>();
        classes.put(Constants.CLASS_OBJECT, getObjectClass());
        classes.put(Constants.CLASS_STRING, getStringClass());
        classes.put(Constants.CLASS_NUMBER, getNumberClass());
        classes.put(Constants.CLASS_EXCEPTION, getExceptionClass());
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
                return new SimiValue.Object(SimiObjectImpl.fromArray(getObjectClass(interpreter), true, self.keys()));
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
                return new SimiValue.Object(SimiObjectImpl.fromArray(getObjectClass(interpreter), true, self.values()));
            }
        });
        methods.put(new OverloadableFunction("enumerate", 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                SimiObjectImpl self = (SimiObjectImpl) arguments.get(0).getObject();
                return new SimiValue.Object(self.enumerate(getObjectClass(interpreter)));
            }
        });
        methods.put(new OverloadableFunction("zip", 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                SimiObjectImpl self = (SimiObjectImpl) arguments.get(0).getObject();
                return new SimiValue.Object(self.zip(getObjectClass(interpreter)));
            }
        });
        methods.put(new OverloadableFunction("append", 1), new SimiCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                SimiObjectImpl self = (SimiObjectImpl) arguments.get(0).getObject();
                self.append(arguments.get(1));
                return arguments.get(0);
            }
        });
        methods.put(new OverloadableFunction("addAll", 1), new SimiCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                SimiObjectImpl self = (SimiObjectImpl) arguments.get(0).getObject();
                SimiObjectImpl obj = (SimiObjectImpl) arguments.get(1).getObject();
                self.addAll(obj);
                return arguments.get(0);
            }
        });
        methods.put(new OverloadableFunction("clear", 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                SimiObjectImpl self = (SimiObjectImpl) arguments.get(0).getObject();
                self.clear((Environment) interpreter.getEnvironment());
                return null;
            }
        });
        methods.put(new OverloadableFunction("indexOf", 1), new SimiCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                SimiObjectImpl self = (SimiObjectImpl) arguments.get(0).getObject();
                SimiValue value = arguments.get(1);
                return self.indexOf(value);
            }
        });
        methods.put(new OverloadableFunction("reversed", 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                SimiObjectImpl self = (SimiObjectImpl) arguments.get(0).getObject();
                return new SimiValue.Object(self.reversed());
            }
        });
        methods.put(new OverloadableFunction("sorted", 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                return sort(interpreter, arguments, null);
            }
        });
        methods.put(new OverloadableFunction("sorted", 1), new SimiCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                SimiCallable comparator = arguments.get(1).getCallable();
                return sort(interpreter, arguments, comparator);
            }
        });
        methods.put(new OverloadableFunction("uniques", 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                SimiObjectImpl self = (SimiObjectImpl) arguments.get(0).getObject();
                if (self.isArray()) {
                    return new SimiValue.Object(SimiObjectImpl.fromArray(getObjectClass(interpreter), self.immutable,
                            self.values().stream().distinct().collect(Collectors.toCollection(ArrayList::new))));
                }
                return arguments.get(0); // Objects are unique by default
            }
        });
        methods.put(new OverloadableFunction("clone", 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                SimiObjectImpl self = (SimiObjectImpl) arguments.get(0).getObject();
                return new SimiValue.Object(self.clone(false));
            }
        });
        methods.put(new OverloadableFunction("clone", 1), new SimiCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                SimiObjectImpl self = (SimiObjectImpl) arguments.get(0).getObject();
                boolean mutable = Interpreter.isTruthy(arguments.get(1));
                return new SimiValue.Object(self.clone(mutable));
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
        methods.put(new OverloadableFunction("class", 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                SimiObject self = arguments.get(0).getObject();
                SimiClass clazz = self.getSimiClass();
                if (clazz == null) {
                    return null;
                }
                return new SimiValue.Object(clazz);
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
                final Iterator<?> iterator = self.iterate();
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
        methods.put(new OverloadableFunction(Constants.EQUALS, 1), new SimiCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                return new SimiValue.Number(arguments.get(0).equals(arguments.get(1)));
            }
        });
        methods.put(new OverloadableFunction(Constants.COMPARE_TO, 1), new SimiCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                return new SimiValue.Number(arguments.get(0).compareTo(arguments.get(1)));
            }
        });
        methods.put(new OverloadableFunction(Constants.TO_STRING, 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                return new SimiValue.String(arguments.get(0).getObject().toString());
            }
        });
        methods.put(new OverloadableFunction("matches", 1), new SimiCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                SimiObjectImpl self = (SimiObjectImpl) arguments.get(0).getObject();
                SimiObjectImpl other = (SimiObjectImpl) arguments.get(1).getObject();
                return new SimiValue.Number(self.matches(other, null));
            }
        });
        methods.put(new OverloadableFunction("matches", 2), new SimiCallable() {
            @Override
            public int arity() {
                return 2;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                SimiObjectImpl self = (SimiObjectImpl) arguments.get(0).getObject();
                SimiObjectImpl other = (SimiObjectImpl) arguments.get(1).getObject();
                List<String> fieldsToMatch = ((SimiObjectImpl) arguments.get(2).getObject()).values().stream()
                        .map(SimiValue::getString)
                        .collect(Collectors.toList());
                return new SimiValue.Number(self.matches(other, fieldsToMatch));
            }
        });
        methods.put(new OverloadableFunction("builder", 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                SimiClassImpl clazz = (SimiClassImpl) arguments.get(0).getObject();
                Set<SimiFunction> constructors = clazz.getConstructors();
                final Set<String> params = constructors.stream()
                        .map(f -> f.declaration.block.params)
                        .flatMap(Collection::stream)
                        .map(t -> t.lexeme)
                        .collect(Collectors.toSet());
                LinkedHashMap<String, SimiValue> fields = new LinkedHashMap<>();
                SimiNativeObject object = new SimiNativeObject(fields);
                SimiValue objectValue = new SimiValue.Object(object);
                List<SimiValue> initArgs = new ArrayList<>();
                for (String param : params) {
                    fields.put(param, new SimiValue.Callable(new SimiCallable() {
                        @Override
                        public int arity() {
                            return 1;
                        }

                        @Override
                        public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                            SimiValue arg = arguments.get(0);
                            fields.put(Constants.PRIVATE + param, arg);
                            initArgs.add(arg);
                            return objectValue;
                        }
                    }, param, object));
                }
                fields.put("build", new SimiValue.Callable(new SimiCallable() {
                    @Override
                    public int arity() {
                        return 0;
                    }

                    @Override
                    public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                        int size = params.size();
                        Optional<SimiFunction> closest = constructors.stream()
                                .min(Comparator.comparingInt(f -> Math.abs(f.arity() - size)));
                        if (closest.isPresent()) {
                            List<String> closestParams = closest.get().declaration.block.params.stream()
                                    .map(t -> t.lexeme).collect(Collectors.toList());
                            List<SimiValue> args = new ArrayList<>();
                            for (String param : closestParams) {
                                args.add(fields.get(Constants.PRIVATE + param));
                            }
                            return clazz.init(interpreter, args);
                        }
                        return clazz.init(interpreter, initArgs);
                    }
                }, "build", object));
                return objectValue;
            }
        });
        methods.put(new OverloadableFunction("array", 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                return new SimiValue.Object(SimiObjectImpl.fromArray(getObjectClass(interpreter), false, new ArrayList<>()));
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
        methods.put(new OverloadableFunction("endsWith", 1), new SimiCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                String value = prepareStringNativeCall(interpreter, arguments);
                String suffix = arguments.get(1).getString();
                return new SimiValue.Number(value.endsWith(suffix));
            }
        });
        methods.put(new OverloadableFunction("format", 1), new SimiCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                String value = prepareStringNativeCall(interpreter, arguments);
                SimiObjectImpl argsObject = (SimiObjectImpl) arguments.get(1).getObject();
                Object[] args = new Object[argsObject.length()];
                int i = 0;
                for (SimiValue simiValue : argsObject.values()) {
                    if (simiValue instanceof SimiValue.Number) {
                        args[i] = simiValue.getNumber();
                    } else {
                        args[1] = simiValue.toString();
                    }
                    i++;
                }
                return new SimiValue.String(String.format(value, args));
            }
        });
        methods.put(new OverloadableFunction("indexOf", 2), new SimiCallable() {
            @Override
            public int arity() {
                return 2;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                String value = prepareStringNativeCall(interpreter, arguments);
                String str = arguments.get(1).getString();
                int index = arguments.get(2).getNumber().intValue();
                int i = value.indexOf(str, index);
                if (i == -1) {
                    return null;
                }
                return new SimiValue.Number(i);
            }
        });
        methods.put(new OverloadableFunction("lastIndexOf", 2), new SimiCallable() {
            @Override
            public int arity() {
                return 2;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                String value = prepareStringNativeCall(interpreter, arguments);
                String str = arguments.get(1).getString();
                int index = arguments.get(2).getNumber().intValue();
                int i = value.lastIndexOf(str, index);
                if (i == -1) {
                    return null;
                }
                return new SimiValue.Number(i);
            }
        });
        methods.put(new OverloadableFunction("removing", 2), new SimiCallable() {
            @Override
            public int arity() {
                return 2;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                String value = prepareStringNativeCall(interpreter, arguments);
                int start = arguments.get(1).getNumber().intValue();
                int stop = arguments.get(2).getNumber().intValue();
                return new SimiValue.String(value.substring(0, start).concat(value.substring(stop, value.length())));
            }
        });
        methods.put(new OverloadableFunction("replacing", 2), new SimiCallable() {
            @Override
            public int arity() {
                return 2;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                String value = prepareStringNativeCall(interpreter, arguments);
                String old = arguments.get(1).getString();
                String newStr = arguments.get(1).getString();
                return new SimiValue.String(value.replace(old, newStr));
            }
        });
        methods.put(new OverloadableFunction("split", 1), new SimiCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                SimiClassImpl objectClass = (SimiClassImpl) interpreter.getGlobal(Constants.CLASS_OBJECT).getObject();
                String value = prepareStringNativeCall(interpreter, arguments);
                String separator = arguments.get(1).getString();
                ArrayList<SimiValue> split = Arrays.stream(value.split(Pattern.quote(separator)))
                        .map(SimiValue.String::new)
                        .collect(Collectors.toCollection(ArrayList::new));
                return new SimiValue.Object(SimiObjectImpl.fromArray(objectClass, true, split));
            }
        });
        methods.put(new OverloadableFunction("startsWith", 1), new SimiCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                String value = prepareStringNativeCall(interpreter, arguments);
                String prefix = arguments.get(1).getString();
                return new SimiValue.Number(value.startsWith(prefix));
            }
        });
        methods.put(new OverloadableFunction("substring", 2), new SimiCallable() {
            @Override
            public int arity() {
                return 2;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                String value = prepareStringNativeCall(interpreter, arguments);
                int start = arguments.get(1).getNumber().intValue();
                int stop = arguments.get(2).getNumber().intValue();
                return new SimiValue.String(value.substring(start, stop));
            }
        });
        methods.put(new OverloadableFunction("lowerCased", 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                String value = prepareStringNativeCall(interpreter, arguments);
                return new SimiValue.String(value.toLowerCase());
            }
        });
        methods.put(new OverloadableFunction("upperCased", 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                String value = prepareStringNativeCall(interpreter, arguments);
                return new SimiValue.String(value.toUpperCase());
            }
        });
        methods.put(new OverloadableFunction("trim", 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                String value = prepareStringNativeCall(interpreter, arguments);
                return new SimiValue.String(value.trim());
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
        methods.put(new OverloadableFunction(Constants.EQUALS, 1), new SimiCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                return new SimiValue.Number(arguments.get(0).equals(arguments.get(1)));
            }
        });
        methods.put(new OverloadableFunction(Constants.COMPARE_TO, 1), new SimiCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                return new SimiValue.Number(arguments.get(0).compareTo(arguments.get(1)));
            }
        });
        methods.put(new OverloadableFunction("toNumber", 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                String string = prepareStringNativeCall(interpreter, arguments);
                try {
                    double number = Double.parseDouble(string);
                    return new SimiValue.Number(number);
                } catch (NumberFormatException e) {
                    SimiException se = new SimiException((SimiClass) interpreter.getEnvironment().tryGet(Constants.EXCEPTION_NUMBER_FORMAT).getObject(),
                            "Invalid number format!");
                    interpreter.raiseException(se);
                    return null;
                }
            }
        });
        methods.put(new OverloadableFunction("builder", 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                LinkedHashMap<String, SimiValue> fields = new LinkedHashMap<>();
                SimiNativeObject object = new SimiNativeObject(fields);
                SimiValue objectValue = new SimiValue.Object(object);
                StringBuilder sb = new StringBuilder();
                fields.put("add", new SimiValue.Callable(new SimiCallable() {
                    @Override
                    public int arity() {
                        return 1;
                    }

                    @Override
                    public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                        SimiValue value = arguments.get(0);
                        sb.append(value.toString());
                        return objectValue;
                    }
                }, "add", object));
                fields.put("build", new SimiValue.Callable(new SimiCallable() {
                    @Override
                    public int arity() {
                        return 0;
                    }

                    @Override
                    public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                        return new SimiValue.String(sb.toString());
                    }
                }, "build", object));
                return objectValue;
            }
        });
        return new SimiNativeClass(Constants.CLASS_STRING, methods);
    }

    private SimiNativeClass getNumberClass() {
        Map<OverloadableFunction, SimiCallable> methods = new HashMap<>();
        methods.put(new OverloadableFunction(Constants.EQUALS, 1), new SimiCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                return new SimiValue.Number(arguments.get(0).equals(arguments.get(1)));
            }
        });
        methods.put(new OverloadableFunction(Constants.COMPARE_TO, 1), new SimiCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                return new SimiValue.Number(arguments.get(0).compareTo(arguments.get(1)));
            }
        });
        methods.put(new OverloadableFunction(Constants.TO_STRING, 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                return new SimiValue.String(arguments.get(0).getNumber().toString());
            }
        });
        return new SimiNativeClass(Constants.CLASS_NUMBER, methods);
    }

    private SimiNativeClass getExceptionClass() {
        Map<OverloadableFunction, SimiCallable> methods = new HashMap<>();
        methods.put(new OverloadableFunction(Constants.RAISE, 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
                SimiObjectImpl self = (SimiObjectImpl) arguments.get(0).getObject();
                String message = self.get("message", interpreter.getEnvironment()).getString();
                interpreter.raiseException(new SimiException(self.clazz, message));
                return null;
            }
        });
        return new SimiNativeClass(Constants.CLASS_EXCEPTION, methods);
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

    private SimiClassImpl getObjectClass(BlockInterpreter interpreter) {
        return (SimiClassImpl) interpreter.getGlobal(Constants.CLASS_OBJECT).getObject();
    }

    private SimiValue sort(BlockInterpreter interpreter,
                           List<SimiValue> arguments,
                           SimiCallable comparator) {
        SimiObjectImpl self = (SimiObjectImpl) arguments.get(0).getObject();
        SimiClassImpl objectClass = getObjectClass(interpreter);
        if (self.isArray()) {
            Comparator<SimiValue> nativeComparator;
            if (comparator == null) {
                nativeComparator = Comparator.naturalOrder();
            } else {
                nativeComparator = (o1, o2) -> comparator.call(interpreter, Arrays.asList(o1, o2)).getNumber().intValue();
            }
            return new SimiValue.Object(self.sorted(nativeComparator));
        } else {
            Comparator<Map.Entry<String, SimiValue>> nativeComparator;
            if (comparator == null) {
                nativeComparator = Comparator.comparing(Map.Entry::getKey);
            } else {
                nativeComparator = (o1, o2) -> comparator.call(interpreter, Arrays.asList(
                        new SimiValue.Object(SimiObjectImpl.decomposedPair(objectClass, new SimiValue.String(o1.getKey()), o1.getValue())),
                        new SimiValue.Object(SimiObjectImpl.decomposedPair(objectClass, new SimiValue.String(o2.getKey()), o2.getValue()))
                    )).getNumber().intValue();
            }
            return new SimiValue.Object(self.sorted(nativeComparator));
        }
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
