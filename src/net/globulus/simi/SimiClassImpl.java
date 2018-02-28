package net.globulus.simi;

import net.globulus.simi.api.BlockInterpreter;
import net.globulus.simi.api.SimiClass;
import net.globulus.simi.api.SimiProperty;
import net.globulus.simi.api.SimiValue;

import java.util.*;
import java.util.stream.Collectors;

class SimiClassImpl extends SimiObjectImpl.Dictionary implements SimiClass {

  final String name;
  final List<SimiClassImpl> superclasses;

  private final Map<OverloadableFunction, SimiFunction> methods;

  static final SimiClassImpl CLASS = new SimiClassImpl(Constants.CLASS_CLASS);

    private SimiClassImpl(String name) {
       super(null, true, new LinkedHashMap<>());
       this.name = name;
       superclasses = null;
       methods = new HashMap<>();
    }

  SimiClassImpl(String name,
                List<SimiClassImpl> superclasses,
                Map<String, SimiProperty> constants,
                Map<OverloadableFunction, SimiFunction> methods) {
    super(CLASS, !name.startsWith(Constants.MUTABLE), new LinkedHashMap<>(constants));
    this.superclasses = superclasses;
    this.name = name;
    this.methods = methods;
  }

    @Override
    SimiProperty get(Token name, Integer arity, Environment environment) {
        SimiProperty prop = super.get(name, arity, environment);
        if (prop != null) {
            return prop;
        }
        SimiMethod method = findMethod(null, name.lexeme, arity);
        if (method != null) {
            return new SimiPropertyImpl(new SimiValue.Callable(method, name.lexeme, this), method.function.annotations);
        }
        if (superclasses != null) {
            for (SimiClassImpl superclass : superclasses) {
                SimiProperty superclassProp = superclass.get(name, arity, environment);
                if (superclassProp != null) {
                    return superclassProp;
                }
            }
        }
        return null;
    }

  SimiMethod findMethod(SimiObjectImpl instance, String name, Integer arity) {
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
                return new SimiMethod(this, methods.get(of).bind(instance));
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
            return new SimiMethod(this, candidate.get().bind(instance));
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
      SimiObjectImpl instance = new SimiObjectImpl.Dictionary(this, true, new LinkedHashMap<>());
      SimiMethod initializer = findMethod(instance, Constants.INIT, arguments.size());
      if (initializer == null) {
          initializer = findMethod(instance, Constants.PRIVATE + Constants.INIT, arguments.size());
      }
      if (initializer != null) {
          if (initializer.function.isNative) {
              Interpreter in = (Interpreter) interpreter;
              instance = (SimiObjectImpl) in.nativeModulesManager.call(name, Constants.INIT, this, in, arguments).getValue().getObject();
          } else {
              initializer.function.call(interpreter, arguments, false);
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

        public SuperClassesList(List<SimiClassImpl> value) {
            this.value = value;
        }

      @Override
      public SimiValue copy() {
          return new SuperClassesList(value);
      }

      @Override
      public SimiValue clone(boolean mutable) {
          throw new AssertionError();
      }

      @Override
      public int compareTo(SimiValue o) {
          throw new AssertionError();
      }
  }
}
