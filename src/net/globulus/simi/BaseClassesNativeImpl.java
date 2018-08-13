package net.globulus.simi;

import net.globulus.simi.api.*;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
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
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                SimiObjectImpl self = (SimiObjectImpl) arguments.get(0).getValue().getObject();
                return new SimiValue.Number(self.length());
            }
        });
        methods.put(new OverloadableFunction("keys", 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                SimiObjectImpl self = (SimiObjectImpl) arguments.get(0).getValue().getObject();
                return new SimiValue.Object(SimiObjectImpl.fromArray(getObjectClass(interpreter), true, self.keys()));
            }
        });
        methods.put(new OverloadableFunction("methods", 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                SimiObjectImpl self = (SimiObjectImpl) arguments.get(0).getValue().getObject();
                SimiClassImpl clazz = (self instanceof SimiClassImpl) ? (SimiClassImpl) self : self.clazz;
                Map<String, ArrayList<SimiProperty>> tempMap = new HashMap<>();
                for (Map.Entry<OverloadableFunction, SimiFunction> method : clazz.methods.entrySet()) {
                    OverloadableFunction key = method.getKey();
                    List<SimiProperty> list = tempMap.computeIfAbsent(key.name, k -> new ArrayList<>());
                    list.add(new SimiPropertyImpl(new SimiValue.Callable(method.getValue(), null, self), method.getValue().annotations));
                }
                LinkedHashMap<String, SimiProperty> map = new LinkedHashMap<>(tempMap.size());
                for (Map.Entry<String, ArrayList<SimiProperty>> entry : tempMap.entrySet()) {
                    map.put(entry.getKey(), new SimiValue.Object(SimiObjectImpl.fromArray(getObjectClass(interpreter), true, entry.getValue())));
                }
                return new SimiValue.Object(SimiObjectImpl.fromMap(getObjectClass(interpreter), true, map));
            }
        });
        methods.put(new OverloadableFunction("values", 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                SimiObjectImpl self = (SimiObjectImpl) arguments.get(0).getValue().getObject();
                return new SimiValue.Object(SimiObjectImpl.fromArray(getObjectClass(interpreter), true, self.values()));
            }
        });
        methods.put(new OverloadableFunction("enumerate", 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                SimiObjectImpl self = (SimiObjectImpl) arguments.get(0).getValue().getObject();
                return new SimiValue.Object(self.enumerate(getObjectClass(interpreter)));
            }
        });
        methods.put(new OverloadableFunction("zip", 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                SimiObjectImpl self = (SimiObjectImpl) arguments.get(0).getValue().getObject();
                return new SimiValue.Object(self.zip(getObjectClass(interpreter)));
            }
        });
        methods.put(new OverloadableFunction("append", 1), new SimiCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                SimiObjectImpl self = (SimiObjectImpl) arguments.get(0).getValue().getObject();
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
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                SimiObjectImpl self = (SimiObjectImpl) arguments.get(0).getValue().getObject();
                SimiObjectImpl obj = (SimiObjectImpl) arguments.get(1).getValue().getObject();
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
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                SimiObjectImpl self = (SimiObjectImpl) arguments.get(0).getValue().getObject();
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
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                SimiObjectImpl self = (SimiObjectImpl) arguments.get(0).getValue().getObject();
                SimiValue value = arguments.get(1).getValue();
                return self.indexOf(value);
            }
        });
        methods.put(new OverloadableFunction("reversed", 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                SimiObjectImpl self = (SimiObjectImpl) arguments.get(0).getValue().getObject();
                return new SimiValue.Object(self.reversed());
            }
        });
        methods.put(new OverloadableFunction("sorted", 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                return sort(interpreter, arguments, null);
            }
        });
        methods.put(new OverloadableFunction("sorted", 1), new SimiCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                SimiCallable comparator = arguments.get(1).getValue().getCallable();
                return sort(interpreter, arguments, comparator);
            }
        });
        methods.put(new OverloadableFunction("uniques", 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                SimiObjectImpl self = (SimiObjectImpl) arguments.get(0).getValue().getObject();
                if (self.isArray()) {
                    return new SimiValue.Object(SimiObjectImpl.fromArray(getObjectClass(interpreter), self.immutable,
                            new ArrayList<>(self.values().stream().distinct().collect(Collectors.toList()))));
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
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                SimiObjectImpl self = (SimiObjectImpl) arguments.get(0).getValue().getObject();
                return new SimiValue.Object(self.clone(false));
            }
        });
        methods.put(new OverloadableFunction("clone", 1), new SimiCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                SimiObjectImpl self = (SimiObjectImpl) arguments.get(0).getValue().getObject();
                boolean mutable = Interpreter.isTruthy(arguments.get(1).getValue());
                return new SimiValue.Object(self.clone(mutable));
            }
        });
        methods.put(new OverloadableFunction("isMutable", 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                return new SimiValue.Number(!((SimiObjectImpl) arguments.get(0).getValue().getObject()).immutable);
            }
        });
        methods.put(new OverloadableFunction("isArray", 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                return new SimiValue.Number(((SimiObjectImpl) arguments.get(0).getValue().getObject()).isArray());
            }
        });
        methods.put(new OverloadableFunction("class", 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                SimiObject self = arguments.get(0).getValue().getObject();
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
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                SimiObjectImpl self = (SimiObjectImpl) arguments.get(0).getValue().getObject();
                final boolean isArray = self.isArray();
                final Iterator<?> iterator = self.iterate();
                LinkedHashMap<String, SimiProperty> fields = new LinkedHashMap<>();
                fields.put(Constants.NEXT, new SimiValue.Callable(new SimiCallable() {
                    @Override
                    public int arity() {
                        return 0;
                    }

                    @Override
                    public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                        if (iterator.hasNext()) {
                            return (SimiProperty) iterator.next();
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
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                SimiObjectImpl self = (SimiObjectImpl) arguments.get(0).getValue().getObject();
                return new SimiValue.Number(self.contains(arguments.get(1).getValue(), Token.nativeCall(Constants.HAS)));
            }
        });
        methods.put(new OverloadableFunction(Constants.EQUALS, 1), new SimiCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                return new SimiValue.Number(arguments.get(0).equals(arguments.get(1)));
            }
        });
        methods.put(new OverloadableFunction(Constants.COMPARE_TO, 1), new SimiCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                return new SimiValue.Number(arguments.get(0).getValue().compareTo(arguments.get(1).getValue()));
            }
        });
        methods.put(new OverloadableFunction(Constants.TO_STRING, 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                return new SimiValue.String(arguments.get(0).getValue().getObject().toString());
            }
        });
        methods.put(new OverloadableFunction("matches", 1), new SimiCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                SimiObjectImpl self = (SimiObjectImpl) arguments.get(0).getValue().getObject();
                SimiObjectImpl other = (SimiObjectImpl) arguments.get(1).getValue().getObject();
                return new SimiValue.Number(self.matches(other, null));
            }
        });
        methods.put(new OverloadableFunction("matches", 2), new SimiCallable() {
            @Override
            public int arity() {
                return 2;
            }

            @Override
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                SimiObjectImpl self = (SimiObjectImpl) arguments.get(0).getValue().getObject();
                SimiObjectImpl other = (SimiObjectImpl) arguments.get(1).getValue().getObject();
                List<String> fieldsToMatch = ((SimiObjectImpl) arguments.get(2).getValue().getObject()).values().stream()
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
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                SimiClassImpl clazz = (SimiClassImpl) arguments.get(0).getValue().getObject();
                Set<SimiFunction> constructors = clazz.getConstructors();
                final Set<String> params = constructors.stream()
                        .map(f -> f.declaration.block.params)
                        .flatMap(Collection::stream)
                        .map(BlockImpl::getParamLexeme)
                        .collect(Collectors.toSet());
                LinkedHashMap<String, SimiProperty> fields = new LinkedHashMap<>();
                SimiNativeObject object = new SimiNativeObject(fields);
                SimiValue objectValue = new SimiValue.Object(object);
                List<SimiProperty> initArgs = new ArrayList<>();
                for (String param : params) {
                    fields.put(param, new SimiValue.Callable(new SimiCallable() {
                        @Override
                        public int arity() {
                            return 1;
                        }

                        @Override
                        public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                            SimiProperty arg = arguments.get(0);
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
                    public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                        int size = params.size();
                        Optional<SimiFunction> closest = constructors.stream()
                                .min(Comparator.comparingInt(f -> Math.abs(f.arity() - size)));
                        if (closest.isPresent()) {
                            List<String> closestParams = closest.get().declaration.block.params.stream()
                                    .map(BlockImpl::getParamLexeme).collect(Collectors.toList());
                            List<SimiProperty> args = new ArrayList<>();
                            for (String param : closestParams) {
                                args.add(fields.get(Constants.PRIVATE + param).getValue());
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
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                return new SimiValue.Object(SimiObjectImpl.fromArray(getObjectClass(interpreter), false, new ArrayList<>()));
            }
        });
        methods.put(new OverloadableFunction("array", 1), new SimiCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                int capacity = arguments.get(1).getValue().getNumber().intValue();
                return new SimiValue.Object(SimiObjectImpl.fromArray(getObjectClass(interpreter), false, new ArrayList<>(capacity)));
            }
        });
        methods.put(new OverloadableFunction("array", 2), new SimiCallable() {
            @Override
            public int arity() {
                return 2;
            }

            @Override
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                int capacity = arguments.get(1).getValue().getNumber().intValue();
                SimiValue fillValue = arguments.get(2).getValue();
                return new SimiValue.Object(SimiObjectImpl.fromArray(getObjectClass(interpreter), false,
                        new ArrayList<>(Collections.nCopies(capacity, fillValue))));
            }
        });
        methods.put(new OverloadableFunction("slice", 2), new SimiCallable() {
            @Override
            public int arity() {
                return 2;
            }

            @Override
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                SimiObjectImpl self = (SimiObjectImpl) arguments.get(0).getValue().getObject();
                int start = arguments.get(1).getValue().getNumber().intValue();
                int stop = arguments.get(2).getValue().getNumber().intValue();
                if (self.isArray()) {
                    return new SimiValue.Object(SimiObjectImpl.fromArray(getObjectClass(interpreter), true,
                        new ArrayList<>(self.asArray().fields.subList(start, stop - 1))));
                }
                return null; // TODO implement for Dictionary
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
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                String value = prepareValueNativeCall(interpreter, arguments).getString();
                return new SimiValue.Number(value.length());
            }
        });
        methods.put(new OverloadableFunction("endsWith", 1), new SimiCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                String value = prepareValueNativeCall(interpreter, arguments).getString();
                String suffix = arguments.get(1).getValue().getString();
                return new SimiValue.Number(value.endsWith(suffix));
            }
        });
        methods.put(new OverloadableFunction("format", 1), new SimiCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                String value = prepareValueNativeCall(interpreter, arguments).getString();
                SimiObjectImpl argsObject = (SimiObjectImpl) arguments.get(1).getValue().getObject();
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
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                String value = prepareValueNativeCall(interpreter, arguments).getString();
                String str = arguments.get(1).getValue().getString();
                int index = arguments.get(2).getValue().getNumber().intValue();
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
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                String value = prepareValueNativeCall(interpreter, arguments).getString();
                String str = arguments.get(1).getValue().getString();
                int index = arguments.get(2).getValue().getNumber().intValue();
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
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                String value = prepareValueNativeCall(interpreter, arguments).getString();
                int start = arguments.get(1).getValue().getNumber().intValue();
                int stop = arguments.get(2).getValue().getNumber().intValue();
                return new SimiValue.String(value.substring(0, start).concat(value.substring(stop, value.length())));
            }
        });
        methods.put(new OverloadableFunction("replacing", 2), new SimiCallable() {
            @Override
            public int arity() {
                return 2;
            }

            @Override
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                String value = prepareValueNativeCall(interpreter, arguments).getString();
                String old = arguments.get(1).getValue().getString();
                String newStr = arguments.get(2).getValue().getString();
                return new SimiValue.String(value.replace(old, newStr));
            }
        });
        methods.put(new OverloadableFunction("split", 1), new SimiCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                SimiClassImpl objectClass = (SimiClassImpl) interpreter.getGlobal(Constants.CLASS_OBJECT).getValue().getObject();
                String value = prepareValueNativeCall(interpreter, arguments).getString();
                String separator = arguments.get(1).getValue().getString();
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
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                String value = prepareValueNativeCall(interpreter, arguments).getString();
                String prefix = arguments.get(1).getValue().getString();
                return new SimiValue.Number(value.startsWith(prefix));
            }
        });
        methods.put(new OverloadableFunction("substring", 2), new SimiCallable() {
            @Override
            public int arity() {
                return 2;
            }

            @Override
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                String value = prepareValueNativeCall(interpreter, arguments).getString();
                int start = arguments.get(1).getValue().getNumber().intValue();
                int stop = arguments.get(2).getValue().getNumber().intValue();
                return new SimiValue.String(value.substring(start, stop));
            }
        });
        methods.put(new OverloadableFunction("lowerCased", 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                String value = prepareValueNativeCall(interpreter, arguments).getString();
                return new SimiValue.String(value.toLowerCase());
            }
        });
        methods.put(new OverloadableFunction("upperCased", 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                String value = prepareValueNativeCall(interpreter, arguments).getString();
                return new SimiValue.String(value.toUpperCase());
            }
        });
        methods.put(new OverloadableFunction("trim", 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                String value = prepareValueNativeCall(interpreter, arguments).getString();
                return new SimiValue.String(value.trim());
            }
        });
        methods.put(new OverloadableFunction("isAlpha", 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                String value = prepareValueNativeCall(interpreter, arguments).getString();
                for (char c : value.toCharArray()) {
                    if (!isAlpha(c)) {
                        return new SimiValue.Number(false);
                    }
                }
                return new SimiValue.Number(true);
            }

            private boolean isAlpha(char c) {
                return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
            }
        });
        methods.put(new OverloadableFunction("isDigit", 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                String value = prepareValueNativeCall(interpreter, arguments).getString();
                for (char c : value.toCharArray()) {
                    if (!isDigit(c)) {
                        return new SimiValue.Number(false);
                    }
                }
                return new SimiValue.Number(true);
            }

            private boolean isDigit(char c) {
                return c >= '0' && c <= '9';
            }
        });
        methods.put(new OverloadableFunction(Constants.ITERATE, 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                String value = prepareValueNativeCall(interpreter, arguments).getString();
                StringCharacterIterator iterator = new StringCharacterIterator(value);
                LinkedHashMap<String, SimiProperty> fields = new LinkedHashMap<>();
                fields.put(Constants.NEXT, new SimiValue.Callable(new SimiCallable() {
                    @Override
                    public int arity() {
                        return 0;
                    }

                    @Override
                    public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
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
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                String value = prepareValueNativeCall(interpreter, arguments).getString();
                String str = arguments.get(1).getValue().getString();
                return new SimiValue.Number(value.contains(str));
            }
        });
        methods.put(new OverloadableFunction(Constants.EQUALS, 1), new SimiCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                return new SimiValue.Number(arguments.get(0).equals(arguments.get(1)));
            }
        });
        methods.put(new OverloadableFunction(Constants.COMPARE_TO, 1), new SimiCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                return new SimiValue.Number(arguments.get(0).getValue().compareTo(arguments.get(1).getValue()));
            }
        });
        methods.put(new OverloadableFunction("toNumber", 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                String string = prepareValueNativeCall(interpreter, arguments).getString();
                try {
                    double number = Double.parseDouble(string);
                    return new SimiValue.Number(number);
                } catch (NumberFormatException e) {
                    SimiException se = new SimiException((SimiClass) interpreter.getEnvironment().tryGet(Constants.EXCEPTION_NUMBER_FORMAT).getValue().getObject(),
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
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                LinkedHashMap<String, SimiProperty> fields = new LinkedHashMap<>();
                SimiNativeObject object = new SimiNativeObject(fields);
                SimiValue objectValue = new SimiValue.Object(object);
                StringBuilder sb = new StringBuilder();
                fields.put("add", new SimiValue.Callable(new SimiCallable() {
                    @Override
                    public int arity() {
                        return 1;
                    }

                    @Override
                    public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                        SimiProperty prop = arguments.get(0);
                        if (prop == null || prop.getValue() == null) {
                            sb.append("nil");
                        } else {
                            sb.append(prop.getValue().toString());
                        }
                        return objectValue;
                    }
                }, "add", object));
                fields.put("build", new SimiValue.Callable(new SimiCallable() {
                    @Override
                    public int arity() {
                        return 0;
                    }

                    @Override
                    public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
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
        methods.put(new OverloadableFunction("randomInt", 1), new SimiCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                int max = arguments.get(1).getValue().getNumber().intValue();
                return new SimiValue.Number(ThreadLocalRandom.current().nextInt(max));
            }
        });
        methods.put(new OverloadableFunction(Constants.EQUALS, 1), new SimiCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                return new SimiValue.Number(arguments.get(0).equals(arguments.get(1)));
            }
        });
        methods.put(new OverloadableFunction(Constants.COMPARE_TO, 1), new SimiCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                return new SimiValue.Number(arguments.get(0).getValue().compareTo(arguments.get(1).getValue()));
            }
        });
        methods.put(new OverloadableFunction(Constants.TO_STRING, 0), new SimiCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                SimiValue value = prepareValueNativeCall(interpreter, arguments);
                return new SimiValue.String(value.getNumber().toString());
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
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                SimiObjectImpl self = (SimiObjectImpl) arguments.get(0).getValue().getObject();
                String message = self.get("message", interpreter.getEnvironment()).getValue().getString();
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
                public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
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
                public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
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
            public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
                double a = arguments.get(1).getValue().getNumber();
                return new SimiValue.Number(Math.round(a));
            }
        });
        return new SimiNativeClass(Constants.CLASS_GLOBALS, methods);
    }

    private SimiClassImpl getObjectClass(BlockInterpreter interpreter) {
        return (SimiClassImpl) interpreter.getGlobal(Constants.CLASS_OBJECT).getValue().getObject();
    }

    private SimiValue sort(BlockInterpreter interpreter,
                           List<SimiProperty> arguments,
                           SimiCallable comparator) {
        SimiObjectImpl self = (SimiObjectImpl) arguments.get(0).getValue().getObject();
        SimiClassImpl objectClass = getObjectClass(interpreter);
        if (self.isArray()) {
            Comparator<SimiValue> nativeComparator;
            if (comparator == null) {
                nativeComparator = Comparator.naturalOrder();
            } else {
                nativeComparator = (o1, o2) -> comparator.call(interpreter, Arrays.asList(o1, o2), false).getValue().getNumber().intValue();
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
                    ), false).getValue().getNumber().intValue();
            }
            return new SimiValue.Object(self.sorted(nativeComparator));
        }
    }

    private SimiValue prepareValueNativeCall(BlockInterpreter interpreter, List<SimiProperty> arguments) {
        SimiProperty arg0 = arguments.get(0);
        SimiObjectImpl self = (SimiObjectImpl) arg0.getValue().getObject();
        SimiEnvironment environment = interpreter.getEnvironment();
        SimiProperty oldSelf = environment.tryGet(Constants.SELF);
        environment.define(Constants.SELF, arg0);
        SimiValue value = self.get(Constants.PRIVATE, environment).getValue();
        if (oldSelf != null) {
            environment.define(Constants.SELF, oldSelf);
        } else {
            environment.define(Constants.SELF, null);
        }
        return value;
    }

    private SimiValue doMath(List<SimiProperty> arguments, BiFunction<Double, Double, Double> op) {
        double a = arguments.get(1).getValue().getNumber();
        double b = arguments.get(2).getValue().getNumber();
        return new SimiValue.Number(op.apply(a, b));
    }

    private SimiValue doMath(List<SimiProperty> arguments, Function<Double, Double> op) {
        double a = arguments.get(1).getValue().getNumber();
        return new SimiValue.Number(op.apply(a));
    }

    SimiNativeClass get(String className) {
        return classes.get(className);
    }

    SimiCallable get(String className, String methodName, int arity) {
        return get(className).methods.get(new OverloadableFunction(methodName, arity));
    }
}
