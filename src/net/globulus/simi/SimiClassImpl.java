package net.globulus.simi;

import com.sun.tools.internal.jxc.ap.Const;
import net.globulus.simi.api.SimiCallable;
import net.globulus.simi.api.BlockInterpreter;
import net.globulus.simi.api.SimiClass;
import net.globulus.simi.api.SimiValue;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class SimiClassImpl extends SimiObjectImpl implements SimiClass, SimiCallable {

  final String name;
  final List<SimiClassImpl> superclasses;

  private final Map<String, SimiFunction> methods;

  static final SimiClassImpl CLASS = new SimiClassImpl(Constants.CLASS_CLASS);

    private SimiClassImpl(String name) {
       super(null, new LinkedHashMap<>(), true);
       this.name = name;
       superclasses = null;
       methods = new HashMap<>();
    }

  SimiClassImpl(String name,
                List<SimiClassImpl> superclasses,
                Map<String, SimiValue> constants,
                Map<String, SimiFunction> methods) {
    super(CLASS, new LinkedHashMap<>(constants), !name.startsWith(Constants.MUTABLE));
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
  public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
    SimiObjectImpl instance = new SimiObjectImpl(this, true);
    SimiFunction initializer = methods.get(Constants.INIT);
    if (initializer != null) {
      initializer.bind(instance).call(interpreter, arguments);
    }
    return new SimiValue.Object(instance);
  }

  @Override
  public int arity() {
    SimiFunction initializer = methods.get(Constants.INIT);
    if (initializer == null) {
        return 0;
    }
    return initializer.arity();
  }

  static class SuperClassesList extends SimiValue {

        public final List<SimiClassImpl> value;

        public SuperClassesList(List<SimiClassImpl> value) {
            this.value = value;
        }
  }
}
