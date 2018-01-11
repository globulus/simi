package net.globulus.simi;

import net.globulus.simi.api.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class SimiObjectImpl implements SimiObject {

  final SimiClassImpl clazz;
  final LinkedHashMap<String, SimiValue> fields;
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

  static SimiObjectImpl pair(SimiClassImpl objectClass, String key, SimiValue value) {
      LinkedHashMap<String, SimiValue> field = new LinkedHashMap<>();
      field.put(key, value);
      return new SimiObjectImpl(objectClass, field, true);
  }

    static SimiObjectImpl decomposedPair(SimiClassImpl objectClass, String key, SimiValue value) {
        LinkedHashMap<String, SimiValue> field = new LinkedHashMap<>();
        field.put(Constants.KEY, new SimiValue.String(key));
        field.put(Constants.VALUE, value);
        return new SimiObjectImpl(objectClass, field, true);
    }

  static SimiObjectImpl fromMap(SimiClassImpl clazz, LinkedHashMap<String, SimiValue> fields) {
      return new SimiObjectImpl(clazz, fields, true);
  }

    static SimiObjectImpl fromArray(SimiClassImpl clazz, List<SimiValue> fields) {
        LinkedHashMap<String, SimiValue> map = new LinkedHashMap<>();
        for (int i = 0; i < fields.size(); i++) {
            map.put(Constants.IMPLICIT + i, fields.get(i));
        }
        return new SimiObjectImpl(clazz, map, true);
    }

  SimiValue get(Token name, Integer arity, Environment environment) {
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
          String implicitKey = Constants.IMPLICIT + index;
          if (fields.containsKey(implicitKey)) {
              return bind(implicitKey, fields.get(implicitKey));
          } else {
              if (clazz != null && clazz.name.equals(Constants.CLASS_STRING)) {
                  String value = fields.get(Constants.PRIVATE).getString();
                  return new SimiValue.String("" + value.charAt(index));
              }
              return bind(implicitKey, new ArrayList<>(fields.values()).get(index));
          }
      } catch (NumberFormatException ignored) { }

      if (key.startsWith(Constants.PRIVATE) && environment.get(Token.self()).getObject() != this) {
          throw new RuntimeError(name, "Trying to access a private property!");
      }
    if (fields.containsKey(key)) {
      return bind(key, fields.get(key));
    }

    if (clazz != null) {
          if (clazz.fields.containsKey(key)) {
              return bind(key, clazz.fields.get(key));
          }
        SimiMethod method = clazz.findMethod(this, key, arity);
        if (method != null) {
            return new SimiValue.Callable(method, key, this);
        }
    }

    return null;
//    throw new RuntimeError(name, // [hidden]
//        "Undefined property '" + name.lexeme + "'.");
  }

  private SimiValue bind(String key, SimiValue value) {
      if (value instanceof SimiValue.Callable) {
          SimiCallable callable = value.getCallable();
          if (callable instanceof BlockImpl) {
              return new SimiValue.Callable(((BlockImpl) callable).bind(this), key, this);
          }
      }
      return value;
  }

  void set(Token name, SimiValue value, Environment environment) {
      checkMutability(name, environment);
      String key = name.lexeme;
      if (key.equals(Constants.PRIVATE) && clazz != null
              && (clazz.name.equals(Constants.CLASS_STRING) || clazz.name.equals(Constants.CLASS_NUMBER))) {
          throw new RuntimeError(name, "Cannot modify self._ of Strings and Numbers!");
      }
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
      if (clazz.name.equals(this.clazz.name)) {
          return true;
      }
      if (this.clazz.superclasses != null) {
          for (SimiClassImpl superclass : this.clazz.superclasses) {
              if (superclass.name.equals(clazz.name)) {
                  return true;
              }
          }
      }
      return false;
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

  @Override
  public SimiObjectImpl clone(boolean mutable) {
      LinkedHashMap<String, SimiValue> fieldsClone = new LinkedHashMap<>();
      for (Map.Entry<String, SimiValue> entry : fields.entrySet()) {
          fieldsClone.put(entry.getKey(), entry.getValue().clone(mutable));
      }
      return new SimiObjectImpl(clazz, fieldsClone, mutable);
  }

  int length() {
      return fields.size();
  }

  private void checkMutability(Token name, Environment environment) {
      if (this.immutable && environment.get(Token.self()).getObject() != this) {
          SimiObject obj = environment.get(Token.self()).getObject();
          throw new RuntimeError(name, "Trying to alter an immutable object!");
      }
  }

  SimiObjectImpl enumerate(SimiClassImpl objectClass) {
      return SimiObjectImpl.fromArray(objectClass, fields.entrySet().stream()
              .map(e -> new SimiValue.Object(SimiObjectImpl.decomposedPair(objectClass, e.getKey(), e.getValue())))
              .collect(Collectors.toList()));
  }

  SimiObjectImpl zip(SimiClassImpl objectClass) {
      if (!isArray()) {
          throw new RuntimeException("Can only zip arrays!");
      }
      if (length() == 0) {
          return new SimiObjectImpl(objectClass, new LinkedHashMap<>(), true);
      }
      LinkedHashMap<String, SimiValue> zipFields = new LinkedHashMap<>();
      for (SimiValue value : fields.values()) {
          SimiObjectImpl obj = (SimiObjectImpl) value.getObject();
          zipFields.put(obj.fields.get(Constants.KEY).getString(), obj.fields.get(Constants.VALUE));
      }
      return new SimiObjectImpl(objectClass, zipFields, true);
  }

  void append(SimiValue elem) {
      if (immutable) {
          throw new RuntimeException("Trying to append to an immutable object!");
      }
      fields.put(Constants.IMPLICIT + fields.size(), elem);
  }

  void addAll(SimiObjectImpl other) {
      if (other.isArray()) {
          for (SimiValue value : other.fields.values()) {
              append(value);
          }
      } else {
          for (Map.Entry<String, SimiValue> entry : other.fields.entrySet()) {
              if (!fields.containsKey(entry.getKey())) {
                  fields.put(entry.getKey(), entry.getValue());
              }
          }
      }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[\n");
    sb.append("\timmutable: ").append(immutable).append("\n");
    if (clazz != null) {
        sb.append("\tclass: ")
                .append(clazz.name)
//                .append("\n\t>>")
//                .append(clazz.toString())
                .append(";\n");
    }
    sb.append(printFields());
    sb.append("]");
    return sb.toString();
  }

  protected String printFields() {
      StringBuilder sb = new StringBuilder();
      for (String key : fields.keySet()) {
          sb.append("\t").append(key).append(" = ").append(fields.get(key)).append("\n");
      }
      return sb.toString();
  }

    @Override
    public SimiClass getSimiClass() {
        return clazz;
    }

    @Override
    public SimiValue get(String key, SimiEnvironment environment) {
        return get(Token.nativeCall(key), null, (Environment) environment);
    }

    @Override
    public void set(String key, SimiValue value, SimiEnvironment environment) {
      set(Token.nativeCall(key), value, (Environment) environment);
    }

    static SimiObject getOrConvertObject(SimiValue value, Interpreter interpreter) {
      if (value instanceof SimiValue.Number || value instanceof SimiValue.String) {
          LinkedHashMap<String, SimiValue> fields = new LinkedHashMap<>();
          fields.put(Constants.PRIVATE, value);
          return new SimiObjectImpl((SimiClassImpl) interpreter.getGlobal(
                    value instanceof SimiValue.Number ? Constants.CLASS_NUMBER : Constants.CLASS_STRING).getObject(),
                  fields, true);
      }
      return value.getObject();
    }
}
