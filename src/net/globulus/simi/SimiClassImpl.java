package net.globulus.simi;

import net.globulus.simi.api.SimiCallable;
import net.globulus.simi.api.SimiInterpreter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class SimiClassImpl extends SimiObjectImpl implements SimiCallable {

  final String name;
  final List<SimiClassImpl> superclasses;

  private final Map<String, SimiFunction> methods;

  SimiClassImpl(String name,
                List<SimiClassImpl> superclasses,
                Map<String, Value> constants,
                Map<String, SimiFunction> methods) {
    super(new LinkedHashMap<>(constants), name.startsWith(Constants.MUTABLE));
    this.superclasses = superclasses;
    this.name = name;
    this.methods = methods;
  }

  SimiFunction findMethod(SimiObjectImpl instance, String name) {
    if (methods.containsKey(name)) {
      return methods.get(name).bind(instance);
    }

    if (superclasses != null) {
        for (SimiClassImpl superclass : superclasses) {
            SimiFunction method  = superclass.findMethod(instance, name);
            if (method != null) {
                return method;
            }
        }
    }

    return null;
  }

  @Override
  public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(name).append(" = [\n");
      sb.append(printFields());
      sb.append("]");
      return sb.toString();
  }

  @Override
  public Object call(SimiInterpreter interpreter, List<Object> arguments, boolean immutable) {
    SimiObjectImpl instance = new SimiObjectImpl(this, immutable);
    SimiFunction initializer = methods.get(Constants.INIT);
    if (initializer != null) {
      initializer.bind(instance).call(interpreter, arguments, immutable);
    }
    return instance;
  }

  @Override
  public int arity() {
    SimiFunction initializer = methods.get(Constants.INIT);
    if (initializer == null) {
        return 0;
    }
    return initializer.arity();
  }
}
