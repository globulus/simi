package net.globulus.simi;

import com.squareup.kotlinpoet.ClassName;
import com.squareup.kotlinpoet.KModifier;
import com.squareup.kotlinpoet.TypeSpec;
import net.globulus.simi.api.*;

import java.util.*;
import java.util.stream.Collectors;

class SimiClassImpl extends SimiObjectImpl implements SimiClass, KotlinCodifiable<TypeSpec.Builder> {

    final Type type;
    final String name;
    final List<SimiClassImpl> superclasses;
    final Stmt.Class stmt;

    final Map<OverloadableFunction, SimiFunction> methods;

    private static final SimiClassImpl CLASS = new SimiClassImpl(Type.REGULAR, Constants.CLASS_CLASS);

    private SimiClassImpl(Type type, String name) {
       super(null, true, new LinkedHashMap<>(), null);
       this.type = type;
       this.name = name;
       superclasses = null;
       methods = new HashMap<>();
       stmt = null;
    }

  SimiClassImpl(Type type,
                String name,
                List<SimiClassImpl> superclasses,
                Map<String, SimiProperty> constants,
                Map<OverloadableFunction, SimiFunction> methods,
                Stmt.Class stmt) {
    super(CLASS, type != Type.OPEN, new LinkedHashMap<>(constants), null);
    this.type = type;
    this.superclasses = superclasses;
    this.name = name;
    this.methods = methods;
    this.stmt = stmt;
  }

    @Override
    SimiProperty get(Token name, Integer arity, Environment environment, boolean forbidPrivate) {
        SimiProperty prop = super.get(name, arity, environment, forbidPrivate);
        if (prop != null) {
            return prop;
        }
        SimiMethod method = findMethod(null, name.lexeme, arity);
        if (method != null) {
            return new SimiPropertyImpl(new SimiValue.Callable(method, name.lexeme, this), method.function.annotations);
        }
        if (superclasses != null) {
            for (SimiClassImpl superclass : superclasses) {
                SimiProperty superclassProp = superclass.get(name, arity, environment, false);
                if (superclassProp != null) {
                    return superclassProp;
                }
            }
        }
        return null;
    }

  SimiMethod findMethod(SimiObjectImpl instance, String name, Integer arity) {
      SimiObjectImpl binder = (instance != null) ? instance : this;
        if (arity == null) {
            Optional<SimiFunction> candidate = methods.entrySet().stream()
                    .filter(e -> e.getKey().name.equals(name))
                    .map(Map.Entry::getValue)
                    .findFirst();
            if (candidate.isPresent()) {
                return new SimiMethod(this, candidate.get());
            }
        } else {
            OverloadableFunction of = new OverloadableFunction(name, arity);
            if (methods.containsKey(of)) {
                return new SimiMethod(this, methods.get(of).bind(binder));
            }
        }

    if (superclasses != null) {
        for (SimiClassImpl superclass : superclasses) {
            SimiMethod method  = superclass.findMethod(instance, name, arity);
            if (method != null) {
                return method;
            }
        }
    }

    if (arity == null) {
        return null;
    } else {
        return findClosestVarargMethod(instance, name, arity);
    }
  }

  private SimiMethod findClosestVarargMethod(SimiObjectImpl instance, String name, int arity) {
        Optional<SimiFunction> candidate = methods.entrySet().stream()
                .filter(e -> e.getKey().name.equals(name) && e.getKey().arity <= arity)
                .sorted(Comparator.comparingInt(l -> Math.abs(l.getKey().arity - arity)))
                .map(Map.Entry::getValue)
                .findFirst();
        if (candidate.isPresent()) {
            return new SimiMethod(this, candidate.get().bind((instance != null) ? instance : this));
        }
        if (superclasses != null) {
          for (SimiClassImpl superclass : superclasses) {
              SimiMethod method  = superclass.findClosestVarargMethod(instance, name, arity);
              if (method != null) {
                  return method;
              }
          }
      }
      return null;
  }

  @Override
  public SimiProperty init(BlockInterpreter interpreter, List<SimiProperty> arguments) {
      SimiObjectImpl instance = SimiObjectImpl.instance(this, null);
      SimiMethod initializer = findMethod(instance, Constants.INIT, arguments.size());
      if (initializer == null) {
          initializer = findMethod(instance, Constants.PRIVATE + Constants.INIT, arguments.size());
      }
      if (initializer != null) {
          if (initializer.function.isNative) {
              Interpreter in = (Interpreter) interpreter;
              for (NativeModulesManager manager : in.nativeModulesManagers) {
                  try {
                      SimiProperty inst = manager.call(name, Constants.INIT, this, in, arguments);
                      if (inst != null) {
                          instance = (SimiObjectImpl) inst.getValue().getObject();
                          if (instance != null) {
                              break;
                          }
                      }
                  } catch (IllegalArgumentException ignored) {
                  }
              }
          } else {
              initializer.function.call(interpreter, null, arguments, false);
          }
      }
      return new SimiValue.Object(instance);
  }

  @Override
  public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(name);
      if (superclasses != null) {
          sb.append("(").append(superclasses.stream().map(c -> c.name).reduce("", (l, r) -> l + ", " + r)).append(")");
      }
      sb.append(" = [\n");
//      for (String key : constants.keySet()) {
//          sb.append("\t").append(key).append(" = ").append(fields.get(key)).append("\n");
//      }
      for (OverloadableFunction key : methods.keySet()) {
          sb.append("\t")
                  .append(key.name).append("(").append(key.arity).append(")")
                  .append(" = ").append(methods.get(key)).append("\n");
      }
      sb.append(printFields());
      sb.append("]");
      return sb.toString();
  }

  Set<SimiFunction> getConstructors() {
        Set<SimiFunction> constructors = methods.entrySet().stream()
                .filter(e -> e.getKey().name.equals(Constants.INIT) || e.getKey().name.equals(Constants.PRIVATE + Constants.INIT))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());
        if (superclasses != null) {
            for (SimiClassImpl superclass : superclasses) {
                constructors.addAll(superclass.getConstructors());
            }
        }
        return constructors;
  }

    @Override
    SimiObjectImpl enumerate(SimiClassImpl objectClass) {
        ArrayList<SimiProperty> values = getEnumeratedValues(objectClass);
        for (Map.Entry<OverloadableFunction, SimiFunction> entry : methods.entrySet()) {
            values.add(new SimiValue.Object(SimiObjectImpl.decomposedPair(objectClass,
                    new SimiValue.String(entry.getKey().name),
                    new SimiPropertyImpl(new SimiValue.Callable(entry.getValue(), entry.getKey().name, this), entry.getValue().annotations)
            )));
        }
        return SimiObjectImpl.fromArray(objectClass, true, values);
    }

  Set<String> allKeys() {
      Set<String> keys = new HashSet<>(fields.keySet());
      if (superclasses != null) {
          for (SimiClassImpl superclass : superclasses) {
              keys.addAll(superclass.allKeys());
          }
      }
      return keys;
  }

  @Override
  public ArrayList<SimiValue> keys() {
    return allKeys().stream().map(SimiValue.String::new).collect(Collectors.toCollection(ArrayList::new));
  }

  @Override
  public String toCode(int indentationLevel, boolean ignoreFirst) {
    if (stmt != null) {
        return stmt.toCode(indentationLevel, ignoreFirst);
    }
    return super.toCode(indentationLevel, ignoreFirst);
  }

    @Override
    public TypeSpec.Builder toKotlinCode(Object... args) {
        TypeSpec.Builder builder = TypeSpec.classBuilder(name);
        if (type != Type.FINAL) {
            builder.addModifiers(KModifier.OPEN);
        }
        if (superclasses != null && !superclasses.isEmpty()) {
            builder.superclass(new ClassName("", superclasses.get(0).name));
            for (int i = 2; i < superclasses.size(); i++) {
                builder.addSuperinterface(new ClassName("", superclasses.get(i).name), "");
            }
        }

        return builder;
    }

//  @Override
//  public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
//      SimiObjectImpl instance = new SimiObjectImpl(this, true);
//      SimiFunction initializer = methods.get(Constants.INIT);
//      if (initializer != null) {
//          initializer.bind(instance).call(interpreter, arguments);
//      }
//      return new SimiValue.Object(instance);
//  }
//
//  @Override
//  public int arity() {
//    SimiFunction initializer = methods.get(Constants.INIT);
//    if (initializer == null) {
//        return 0;
//    }
//    return initializer.arity();
//  }

  static class SuperClassesList extends SimiValue {

        public final List<SimiClassImpl> value;

        SuperClassesList(List<SimiClassImpl> value) {
            this.value = value;
        }

      @Override
      public SimiValue copy() {
          return new SuperClassesList(value);
      }

      @Override
      public SimiValue clone(boolean mutable) {
          return copy();
      }

      @Override
      public int compareTo(SimiValue o) {
          throw new AssertionError();
      }

      @Override
      public java.lang.String toCode(int indentationLevel, boolean ignoreFirst) {
          return null;
      }
  }

  enum Type {
        REGULAR(TokenType.CLASS), FINAL(TokenType.CLASS_FINAL), OPEN(TokenType.CLASS_OPEN);

        final TokenType tokenType;

        Type(TokenType tokenType) {
            this.tokenType = tokenType;
        }

        static Type from(TokenType tokenType) {
            for (Type type : values()) {
                if (type.tokenType == tokenType) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Illegal token type: " + tokenType);
        }
  }
}
