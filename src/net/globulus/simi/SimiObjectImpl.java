package net.globulus.simi;

import net.globulus.simi.api.*;

import java.util.*;
import java.util.stream.Collectors;

class SimiObjectImpl implements SimiObject {

    final SimiClassImpl clazz;
    final boolean immutable;
    final LinkedHashMap<String, SimiProperty> fields;
    final ArrayList<SimiProperty> line;

  SimiObjectImpl(SimiClassImpl clazz,
                 boolean immutable,
                 LinkedHashMap<String, SimiProperty> fields,
                 ArrayList<SimiProperty> line) {
    this.clazz = clazz;
    this.immutable = immutable;
    this.fields = (fields != null) ? fields : new LinkedHashMap<>();
    this.line = (line != null) ? line : new ArrayList<>();
  }

  static SimiObjectImpl instance(SimiClassImpl clazz, LinkedHashMap<String, SimiProperty> props) {
      return fromMap(clazz, true, props);
  }

  static SimiObjectImpl pair(SimiClassImpl objectClass, String key, SimiProperty prop) {
      LinkedHashMap<String, SimiProperty> field = new LinkedHashMap<>();
      field.put(key, prop);
      return new SimiObjectImpl(objectClass, true, field, null);
  }

    static SimiObjectImpl decomposedPair(SimiClassImpl objectClass, SimiValue key, SimiProperty value) {
        LinkedHashMap<String, SimiProperty> prop = new LinkedHashMap<>();
        prop.put(Constants.KEY, key);
        prop.put(Constants.VALUE, value);
        return new SimiObjectImpl(objectClass, true, prop, null);
    }

  static SimiObjectImpl fromMap(SimiClassImpl clazz, boolean immutable, LinkedHashMap<String, SimiProperty> props) {
      return new SimiObjectImpl(clazz, immutable, props, null);
  }

    static SimiObjectImpl fromArray(SimiClassImpl clazz, boolean immutable, ArrayList<? extends SimiProperty> line) {
        return new SimiObjectImpl(clazz, immutable, null, new ArrayList<>(line));
    }

    SimiProperty get(Token name, Integer arity, Environment environment) {
      return get(name, arity, environment, true);
    }

  SimiProperty get(Token name, Integer arity, Environment environment, boolean forbidPrivate) {
      String key = name.lexeme;

      if (key.equals(Constants.CLASS)) { // class is a special key available to all objects
          return new SimiValue.Object(clazz);
      }

      try {
          int index = Integer.parseInt(key);
          if (index < line.size()) {
              return bind(key, line.get(index)); // If line isn't empty, get a numerical index from line
          }

          String implicitKey = Constants.IMPLICIT + index;
          if (fields.containsKey(implicitKey)) {
              return bind(implicitKey, fields.get(implicitKey));
          } else {
              if (clazz != null && clazz.name.equals(Constants.CLASS_STRING)) {
                  String value = fields.get(Constants.PRIVATE).getValue().getString();
                  return new SimiValue.String("" + value.charAt(index));
              }
              List<SimiProperty> values = new ArrayList<>(fields.values());
              if (values.size() > index) {
                  return bind(implicitKey, values.get(index));
              }
              return null;
          }
      } catch (NumberFormatException ignored) { }

      if (fields.containsKey(key)) {
          if (key.startsWith(Constants.PRIVATE)) {
              SimiObject self = environment.get(Token.self()).getObject();
              if (forbidPrivate && self != this && self != clazz) {
                  throw new RuntimeError(name, "Trying to access a private property!");
              }
          }
          return bind(key, fields.get(key));
      }

      return getFromClass(name, arity, environment, forbidPrivate);
  }

  private SimiProperty getFromClass(Token name, Integer arity, Environment environment, boolean forbidPrivate) {
      String key = name.lexeme;
      if (clazz != null) {
          if (clazz.fields.containsKey(key)) {
              return bind(key, clazz.fields.get(key));
          }
          SimiMethod method = clazz.findMethod(this, key, arity);
          if (method != null) {
              return new SimiPropertyImpl(new SimiValue.Callable(method, key, this), method.function.annotations);
          }
          return clazz.get(name, arity, environment, forbidPrivate);
      }
      return null;
  }

  private SimiProperty bind(String key, SimiProperty prop) {
      if (prop != null && prop.getValue() instanceof SimiValue.Callable) {
          SimiCallable callable = prop.getValue().getCallable();
          if (callable instanceof BlockImpl) {
              return new SimiPropertyImpl(new SimiValue.Callable(((BlockImpl) callable).bind(this), key, this), prop.getAnnotations());
          }
      }
      return prop;
  }

  void set(Token name, SimiProperty prop, Environment environment) {
      if (name == null) {
          return;
      }
      checkMutability(name, environment);
      String key = name.lexeme;
      if (key.equals(Constants.PRIVATE) && clazz != null
              && (clazz.name.equals(Constants.CLASS_STRING) || clazz.name.equals(Constants.CLASS_NUMBER))) {
          throw new RuntimeError(name, "Cannot modify self._ of Strings and Numbers!");
      }
      if (!isArray()) {
          try {
              int index = Integer.parseInt(key);
              key = Constants.IMPLICIT + index;
          } catch (NumberFormatException ignored) { }
      }
      if (key.startsWith(Constants.PRIVATE)) {
           SimiObject self = environment.get(Token.self()).getObject();
           if (self != this && self != clazz) {
               throw new RuntimeError(name, "Trying to access a private property!");
           }
      }
     setField(key, prop);
  }

  private void setField(String key, SimiProperty prop) {
      try {
          int index = Integer.parseInt(key);
          if (prop == null) {
              line.remove(index);
          } else {
              line.set(index, prop);
          }
      } catch (NumberFormatException ignored) {
          if (prop == null) {
              fields.remove(key);
          } else {
              fields.put(key, prop);
          }
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

    private boolean isSupertype(SimiClassImpl clazz) {
        if (clazz.name.equals(this.clazz.name)) {
            return true;
        }
        if (clazz.superclasses != null) {
            for (SimiClassImpl superclass : clazz.superclasses) {
                if (superclass.name.equals(this.clazz.name)) {
                    return true;
                }
            }
        }
        return false;
    }

  boolean isNumber() {
      return clazz.name.equals(Constants.CLASS_NUMBER);
  }

  boolean isString() {
      return clazz.name.equals(Constants.CLASS_STRING);
  }

  void clear(Environment environment) {
      checkMutability(Token.self(), environment);
      fields.clear();
      line.clear();
  }

    boolean matches(SimiObjectImpl other, List<String> fieldsToMatch) {
      if (isArray() && other.isArray()) { // We compare lines only if both objects are pure arrays
          int length = length();
          if (other.length() != length) {
              return false;
          }
          for (int i = 0; i < length; i++) {
              if (!valuesMatch(line.get(i), other.line.get(i))) {
                  return false;
              }
          }
      } else {
          for (Map.Entry<String, SimiProperty> entry : fields.entrySet()) {
              if (fieldsToMatch != null && !fieldsToMatch.contains(entry.getKey())) {
                  continue;
              }
              SimiProperty prop = other.fields.get(entry.getKey());
              if (!valuesMatch(prop, entry.getValue())) {
                  return false;
              }
          }
      }
      return true;
    }

    boolean contains(SimiValue object) {
        return (object instanceof SimiValue.String && fields.keySet().contains(object.getString()))
                || line.contains(object);
    }

    SimiObjectImpl getLine() {
        return new SimiObjectImpl(clazz, immutable, null, line);
    }

    boolean isArray() {
      return fields.isEmpty();
    }

    int length() {
      return fields.size() + line.size();
    }

    private void checkMutability(Token name, SimiEnvironment environment) {
        boolean invalidMutation = false;
        if (this.immutable) {
            SimiValue self = ((Environment) environment).get(Token.self());
              if (self != null) {
                  SimiObject selfObj = self.getObject();
                  if (this != selfObj) {
                      if (selfObj instanceof SimiClassImpl) {
                          invalidMutation = !isSupertype((SimiClassImpl) selfObj);
                      } else {
                          invalidMutation = true;
                      }
                  }
            }
        }
        if (invalidMutation) {
            throw new RuntimeError(name, "Trying to alter an immutable object!");
        }
    }

    public ArrayList<SimiValue> keys() {
        Set<String> keys = new HashSet<>(fields.keySet());
        if (clazz != null) {
            keys.addAll(clazz.allKeys());
        }
        return keys.stream().map(SimiValue.String::new).collect(Collectors.toCollection(ArrayList::new));
    }

    public ArrayList<SimiValue> values() {
        Collection<SimiProperty> values = new ArrayList<>(fields.values());
        values.addAll(line);
        return values.stream().filter(Objects::nonNull).map(SimiProperty::getValue).collect(Collectors.toCollection(ArrayList::new));
    }

    SimiObjectImpl enumerate(SimiClassImpl objectClass) {
        return SimiObjectImpl.fromArray(objectClass, true, getEnumeratedValues(objectClass));
    }

    ArrayList<SimiProperty> getEnumeratedValues(SimiClassImpl objectClass) {
        ArrayList<SimiProperty> values = new ArrayList<>(length());
        for (Map.Entry<String, SimiProperty> entry : fields.entrySet()) {
            values.add(new SimiValue.Object(SimiObjectImpl.decomposedPair(objectClass,
                    new SimiValue.String(entry.getKey()), entry.getValue())));
        }
        int lineSize = line.size();
        for (int i = 0; i < lineSize; i++) {
            values.add(new SimiValue.Object(SimiObjectImpl.decomposedPair(objectClass, new SimiValue.Number(i), line.get(i))));
        }
        return values;
    }

    SimiObjectImpl zip(SimiClassImpl objectClass) {
        LinkedHashMap<String, SimiProperty> zipFields = new LinkedHashMap<>();
        for (SimiProperty prop : fields.values()) {
            SimiObjectImpl obj = (SimiObjectImpl) prop.getValue().getObject();
            zipFields.put(obj.fields.get(Constants.KEY).getValue().getString(), obj.fields.get(Constants.VALUE));
        }
        return fromMap(objectClass, true, zipFields);
    }

    SimiValue indexOf(SimiValue value) {
      if (isArray()) {
          int index = line.indexOf(value);
          if (index == -1) {
              return null;
          }
          return new SimiValue.Number(index);
      } else {
          int index = fields.values().stream().map(SimiProperty::getValue).collect(Collectors.toList()).indexOf(value);
          if (index == -1) {
              return null;
          }
          return new SimiValue.Number(index);
      }
    }

    SimiObjectImpl reversed() {
        ListIterator<Map.Entry<String, SimiProperty>> iter =
                new ArrayList<>(fields.entrySet()).listIterator(fields.size());
        LinkedHashMap<String, SimiProperty> reversedFields = new LinkedHashMap<>();
        while (iter.hasPrevious()) {
            Map.Entry<String, SimiProperty> entry = iter.previous();
            reversedFields.put(entry.getKey(), entry.getValue());
        }
        ArrayList<SimiProperty> reversedArray = new ArrayList<>(line);
        Collections.reverse(reversedArray);
        return new SimiObjectImpl(clazz, immutable, reversedFields, reversedArray);
    }

    Iterator<?> iterate() {
      return isArray() ? line.iterator() : keys().iterator();
    }

    SimiObjectImpl sorted(Comparator<? super Map.Entry<String, SimiProperty>> comparator) {
        LinkedHashMap<String, SimiProperty> sortedFields = new LinkedHashMap<>();
        Comparator<? super Map.Entry<String, SimiProperty>> fieldComp = (comparator != null) ? comparator : Comparator.comparing(Map.Entry::getKey);
        fields.entrySet().stream()
                .sorted(fieldComp)
                .forEach(e -> sortedFields.put(e.getKey(), e.getValue()));
        Comparator<? super SimiProperty> lineComp = (comparator != null)
                ? (o1, o2) -> comparator.compare(new AbstractMap.SimpleEntry<>(Constants.IMPLICIT, o1), new AbstractMap.SimpleEntry<>(Constants.IMPLICIT, o2))
                : Comparator.comparing(SimiProperty::getValue);
        ArrayList<SimiProperty> sortedLine = new ArrayList<>(line);
        sortedLine.sort(lineComp);
        return new SimiObjectImpl(clazz, immutable, sortedFields, sortedLine);
    }

    private boolean valuesMatch(SimiValue a, SimiValue b) {
      if (a instanceof SimiValue.Object) {
          if (!(b instanceof SimiValue.Object)) {
              return false;
          }
          SimiObjectImpl object = (SimiObjectImpl) a.getObject();
          return object.matches((SimiObjectImpl) b.getObject(), null);
      } else {
          return a.equals(b);
      }
    }

    private boolean valuesMatch(SimiProperty a, SimiProperty b) {
      if (a == null && b == null) {
          return true;
      }
      if (a == null || b == null) {
          return false;
      }
      return valuesMatch(a.getValue(), b.getValue());
    }

  void append(SimiProperty elem, SimiEnvironment environment) {
      checkMutability(Token.self(), environment);
      line.add(elem);
  }

  void addAll(SimiObjectImpl other, SimiEnvironment environment) {
      checkMutability(Token.self(), environment);
      for (Map.Entry<String, SimiProperty> entry : other.fields.entrySet()) {
          if (!fields.containsKey(entry.getKey())) {
              fields.put(entry.getKey(), entry.getValue());
          }
      }
      line.addAll(other.line);
  }

  void insertAt(SimiProperty location, SimiProperty elem, SimiEnvironment environment) {
      checkMutability(Token.self(), environment);
      line.add(Math.toIntExact(location.getValue().getNumber().asLong()), elem);
  }

  @Override
  public String toString() {
      if (clazz != null) {
          SimiMethod method = clazz.findMethod(this, Constants.TO_STRING, 0);
          if (method != null && !method.function.isNative) {
              return method.call(Interpreter.sharedInstance, null, new ArrayList<>(), false).getValue().getString();
          }
      }
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

    String printFields() {
      StringBuilder sb = new StringBuilder();
        for (String key : fields.keySet()) {
            sb.append("\t").append(key).append(" = ").append(fields.get(key)).append("\n");
        }
        for (int i = 0; i < line.size(); i++) {
            sb.append("\t").append(i).append(" = ").append(line.get(i)).append("\n");
        }
      return sb.toString();
    }

    @Override
    public SimiClass getSimiClass() {
        return clazz;
    }

    @Override
    public SimiProperty get(String key, SimiEnvironment environment) {
        if (key.equals(Constants.CLASS)) {
            return new SimiValue.Object(clazz);
        }
        return get(Token.nativeCall(key), null, (Environment) environment, true);
    }

    @Override
    public void set(String key, SimiProperty prop, SimiEnvironment environment) {
      set(Token.nativeCall(key), prop, (Environment) environment);
    }

    static SimiObject getOrConvertObject(SimiProperty prop, Interpreter interpreter) {
        if (prop == null) {
            return null;
        }
        SimiValue value = prop.getValue();
        if (value == null || value instanceof SimiValue.Callable) {
            return null;
        }
      if (value instanceof SimiValue.Number || value instanceof SimiValue.String) {
          LinkedHashMap<String, SimiProperty> fields = new LinkedHashMap<>();
          fields.put(Constants.PRIVATE, value);
          return SimiObjectImpl.fromMap((SimiClassImpl) interpreter.getGlobal(
                    value instanceof SimiValue.Number ? Constants.CLASS_NUMBER : Constants.CLASS_STRING).getValue().getObject(),
                  true, fields);
      }
      return value.getObject();
    }

    @Override
    public SimiObject clone(boolean mutable) {
        LinkedHashMap<String, SimiProperty> fieldsClone = new LinkedHashMap<>();
        for (Map.Entry<String, SimiProperty> entry : fields.entrySet()) {
            SimiProperty value = entry.getValue();
            fieldsClone.put(entry.getKey(), (value != null) ? value.clone(mutable) : null);
        }
        ArrayList<SimiProperty> lineClone = new ArrayList<>();
        for (SimiProperty field : line) {
            lineClone.add((field != null) ? field.clone(mutable) : null);
        }
        return new SimiObjectImpl(clazz, mutable, fieldsClone, lineClone);
    }

    @Override
    public String toCode(int indentationLevel, boolean ignoreFirst) {
        String indentation = Codifiable.getIndentation(indentationLevel + 1);
        boolean isArray = isArray();
        return new StringBuilder(ignoreFirst ? "" : Codifiable.getIndentation(indentationLevel))
                .append(immutable ? TokenType.LEFT_BRACKET.toCode() : TokenType.DOLLAR_LEFT_BRACKET.toCode())
                .append(isArray ? "" : TokenType.NEWLINE.toCode())
                .append((clazz != null && !clazz.name.equals(Constants.CLASS_OBJECT))
                        ? (indentation
                        + "\"class\" = gu \"" + clazz.name + "\"" + (fields.isEmpty() ? "" : TokenType.COMMA.toCode())
                        + TokenType.NEWLINE.toCode())
                        : ""
                )
                .append(fields.entrySet().stream()
                        .map(e -> indentation + e.getKey() + " "
                                + TokenType.EQUAL.toCode() + " "
                                + e.getValue().getValue().toCode(indentationLevel + 1, true)
                        )
                        .collect(Collectors.joining(TokenType.COMMA.toCode() + TokenType.NEWLINE.toCode()))
                )
                .append((fields.isEmpty() || line.isEmpty()) ? "" : TokenType.COMMA.toCode())
                .append(line.stream()
                        .map(i -> (i == null) ? "nil" : i.getValue().toCode(indentationLevel + 1, false))
                        .collect(Collectors.joining(TokenType.COMMA.toCode() + " "))
                )
                .append(isArray ? "" : TokenType.NEWLINE.toCode(indentationLevel, false))
                .append(TokenType.RIGHT_BRACKET.toCode(indentationLevel, false))
                .append(TokenType.NEWLINE.toCode())
                .toString();
    }

    @Override
    public int getLineNumber() {
        return -1;
    }

    @Override
    public String getFileName() {
        return null;
    }

    @Override
    public boolean hasBreakPoint() {
        return false;
    }

    @Override
    public int compareTo(SimiObject o) {
        SimiObjectImpl other = (SimiObjectImpl) o;
        Iterator<Map.Entry<String, SimiProperty>> it = fields.entrySet().iterator();
        Iterator<Map.Entry<String, SimiProperty>> oit = other.fields.entrySet().iterator();
        for (int len = Math.min(fields.size(), other.fields.size()); len > 0; len--) {
            Map.Entry<String, SimiProperty> entry = it.next();
            Map.Entry<String, SimiProperty> oentry = oit.next();
            int keyComp = entry.getKey().compareTo(oentry.getKey());
            if (keyComp == 0) {
                int valComp = entry.getValue().getValue().compareTo(oentry.getValue().getValue());
                if (valComp != 0) {
                    return valComp;
                }
            } else {
                return keyComp;
            }
        }
        int len = Math.min(line.size(), other.line.size());
        for (int i = 0; i < len; i++) {
            int valComp = line.get(i).getValue().compareTo(other.line.get(i).getValue());
            if (valComp != 0) {
                return valComp;
            }
        }
        return 0;
    }
}
