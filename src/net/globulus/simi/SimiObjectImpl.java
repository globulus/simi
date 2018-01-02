package net.globulus.simi;

import net.globulus.simi.api.SimiClass;
import net.globulus.simi.api.SimiEnvironment;
import net.globulus.simi.api.SimiObject;
import net.globulus.simi.api.SimiValue;

import java.util.LinkedHashMap;

class SimiObjectImpl implements SimiObject {

  final SimiClassImpl clazz;
  private final LinkedHashMap<String, SimiValue> fields;
  final boolean immutable;

  SimiObjectImpl(SimiClassImpl clazz,
                 LinkedHashMap<String, SimiValue> fields,
                 boolean immutable) {
      this.clazz = clazz;
      this.fields = fields;
      this.immutable = immutable;
  }

  SimiObjectImpl(SimiClassImpl clazz, boolean immutable) {
    this.clazz = clazz;
    this.fields = new LinkedHashMap<>();
    this.immutable = immutable;
  }

  SimiValue get(Token name, Environment environment) {
      String key = name.lexeme;
      try {
          int index = Integer.parseInt(key);
//          int size = fields.size();
//          if (index < 0) {
//              index += size;
//          }
//          for (Map.Entry<String, Object> entry : fields.entrySet()) {
//              if (index == 0) {
//                  return entry.getValue();
//              }
//              index--;
//          }
//          throw new RuntimeError(name, "Invalid index!");
          return fields.get(Constants.IMPLICIT + index);
      } catch (NumberFormatException ignored) { }

      if (key.startsWith(Constants.PRIVATE) && environment.get(Token.self()).getObject() != this) {
          throw new RuntimeError(name, "Trying to access a private property!");
      }
    if (fields.containsKey(key)) {
      return fields.get(key);
    }

    if (clazz != null) {
        SimiFunction method = clazz.findMethod(this, key);
        if (method != null) {
            return new SimiValue.Callable(method);
        }
    }

    return null;
//    throw new RuntimeError(name, // [hidden]
//        "Undefined property '" + name.lexeme + "'.");
  }

  void set(Token name, SimiValue value, Environment environment) {
      checkMutability(name, environment);
      String key = name.lexeme;
      try {
          int index = Integer.parseInt(key);
          key = Constants.IMPLICIT + index;
      } catch (NumberFormatException ignored) { }
      if (key.startsWith(Constants.PRIVATE) && environment.get(Token.self()).getObject() != this) {
          throw new RuntimeError(name, "Trying to access a private property!");
      }
      if (value == null) {
          fields.remove(key);
      } else {
          fields.put(key, value);
      }
  }

  boolean is(SimiClassImpl clazz) {
      return clazz.name.equals(this.clazz.name);
  }

  boolean contains(SimiValue object, Token at) {
      if (isArray()) {
          return fields.values().contains(object);
      }
      if (!(object instanceof SimiValue.String)) {
          throw new RuntimeError(at, "Left side must be a string!");
      }
      return fields.keySet().contains(object.getString());
  }

  boolean isNumber() {
      return clazz.name.equals(Constants.CLASS_NUMBER);
  }

  boolean isString() {
      return clazz.name.equals(Constants.CLASS_STRING);
  }

  boolean isArray() {
      for (String key : fields.keySet()) {
          if (!key.startsWith(Constants.IMPLICIT)) {
              return false;
          }
      }
      return true;
  }

  private void checkMutability(Token name, Environment environment) {
      if (this.immutable && environment.get(Token.self()).getObject() != this) {
          throw new RuntimeError(name, "Trying to alter an immutable object!");
      }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[\n");
    sb.append("immutable").append(immutable);
    if (clazz != null) {
        sb.append("class: ")
                .append(clazz.name)
                .append("\n>>")
                .append(clazz.toString())
                .append("\n");
    }
    sb.append(printFields());
    sb.append("]");
    return sb.toString();
  }

  protected String printFields() {
      StringBuilder sb = new StringBuilder();
      for (String key : fields.keySet()) {
          sb.append(key).append(" = ").append(fields.get(key)).append("\n");
      }
      return sb.toString();
  }

    @Override
    public SimiClass getSimiClass() {
        return clazz;
    }

    @Override
    public SimiValue get(String key, SimiEnvironment environment) {
        return get(Token.nativeCall(key), (Environment) environment);
    }

    @Override
    public void set(String key, SimiValue value, SimiEnvironment environment) {
      set(Token.nativeCall(key), value, (Environment) environment);
    }
}
