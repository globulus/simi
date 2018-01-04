package net.globulus.simi;

import net.globulus.simi.api.*;

import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

class SimiClassImpl extends SimiObjectImpl implements SimiClass {

  final String name;
  final List<SimiClassImpl> superclasses;

  private final Map<OverloadableFunction, SimiFunction> methods;

  static final SimiClassImpl CLASS = new SimiClassImpl(Constants.CLASS_CLASS);
  static final SimiClassImpl NUMBER = new SimiClassImpl(Constants.CLASS_NUMBER);
  static final SimiClassImpl STRING = new SimiClassImpl(Constants.CLASS_STRING);

    private SimiClassImpl(String name) {
       super(null, new LinkedHashMap<>(), true);
       this.name = name;
       superclasses = null;
       methods = new HashMap<>();
    }

  SimiClassImpl(String name,
                List<SimiClassImpl> superclasses,
                Map<String, SimiValue> constants,
                Map<OverloadableFunction, SimiFunction> methods) {
    super(CLASS, new LinkedHashMap<>(constants), !name.startsWith(Constants.MUTABLE));
    this.superclasses = superclasses;
    this.name = name;
    this.methods = methods;
  }

  SimiFunction findMethod(SimiObjectImpl instance, String name, Integer arity) {
        if (arity == null) {
            Optional<SimiFunction> candidate = methods.entrySet().stream()
                    .filter(e -> e.getKey().name.equals(name))
                    .map(Map.Entry::getValue)
                    .findFirst();
            if (candidate.isPresent()) {
                return candidate.get();
            }
        } else {
            OverloadableFunction of = new OverloadableFunction(name, arity);
            if (methods.containsKey(of)) {
                return methods.get(of).bind(instance);
            }
        }

    if (superclasses != null) {
        for (SimiClassImpl superclass : superclasses) {
            SimiFunction method  = superclass.findMethod(instance, name, arity);
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

  private SimiFunction findClosestVarargMethod(SimiObjectImpl instance, String name, int arity) {
        Optional<SimiFunction> candidate = methods.entrySet().stream()
                .filter(e -> e.getKey().name.equals(name) && e.getKey().arity <= arity)
                .sorted(Comparator.comparingInt(l -> Math.abs(l.getKey().arity - arity)))
                .map(Map.Entry::getValue)
                .findFirst();
        if (candidate.isPresent()) {
            return candidate.get().bind(instance);
        }
        if (superclasses != null) {
          for (SimiClassImpl superclass : superclasses) {
              SimiFunction method  = superclass.findClosestVarargMethod(instance, name, arity);
              if (method != null) {
                  return method;
              }
          }
      }
      return null;
  }

  SimiValue init(BlockInterpreter interpreter, List<SimiValue> arguments) {
      SimiObjectImpl instance = new SimiObjectImpl(this, true);
      SimiFunction initializer = findMethod(instance, Constants.INIT, arguments.size());
      if (initializer != null) {
          initializer.call(interpreter, arguments);
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
  }
}
