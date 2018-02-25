package net.globulus.simi;

import net.globulus.simi.api.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

abstract class SimiObjectImpl implements SimiObject {

  final SimiClassImpl clazz;
  final boolean immutable;

  private SimiObjectImpl(SimiClassImpl clazz, boolean immutable) {
    this.clazz = clazz;
    this.immutable = immutable;
  }

  static SimiObjectImpl instance(SimiClassImpl clazz, LinkedHashMap<String, SimiProperty> props) {
      return new Dictionary(clazz, true, props);
  }

  static SimiObjectImpl pair(SimiClassImpl objectClass, String key, SimiProperty prop) {
      LinkedHashMap<String, SimiProperty> field = new LinkedHashMap<>();
      field.put(key, prop);
      return new Dictionary(objectClass, true, field);
  }

    static SimiObjectImpl decomposedPair(SimiClassImpl objectClass, SimiValue key, SimiValue value) {
        LinkedHashMap<String, SimiProperty> prop = new LinkedHashMap<>();
        prop.put(Constants.KEY, new SimiProperty(key));
        prop.put(Constants.VALUE, new SimiProperty(value));
        return new Dictionary(objectClass, true, prop);
    }

  static SimiObjectImpl fromMap(SimiClassImpl clazz, boolean immutable, LinkedHashMap<String, SimiProperty> props) {
      return new Dictionary(clazz, immutable, props);
  }

    static SimiObjectImpl fromArray(SimiClassImpl clazz, boolean immutable, ArrayList<SimiValue> fields) {
        return new Array(clazz, immutable, fields);
    }

    static SimiObjectImpl empty(SimiClassImpl clazz, boolean immutable) {
      return new InitiallyEmpty(clazz, immutable);
    }

  abstract SimiProperty get(Token name, Integer arity, Environment environment);

  SimiProperty getFromClass(Token name, Integer arity) {
      String key = name.lexeme;
      if (clazz != null) {
          if (clazz.fields.containsKey(key)) {
              return bind(key, clazz.fields.get(key));
          }
          SimiMethod method = clazz.findMethod(this, key, arity);
          if (method != null) {
              return new SimiProperty(new SimiValue.Callable(method, key, this), method.function.annotations);
          }
      }
      return null;
  }

  SimiProperty bind(String key, SimiProperty prop) {
      if (prop != null && prop.value instanceof SimiValue.Callable) {
          SimiCallable callable = prop.value.getCallable();
          if (callable instanceof BlockImpl) {
              return new SimiProperty(new SimiValue.Callable(((BlockImpl) callable).bind(this), key, this), prop.annotations);
          }
      }
      return prop;
  }

  void set(Token name, SimiProperty prop, Environment environment) {
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
     setField(key, prop);
  }

  abstract void setField(String key, SimiProperty prop);

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


  boolean isNumber() {
      return clazz.name.equals(Constants.CLASS_NUMBER);
  }

  boolean isString() {
      return clazz.name.equals(Constants.CLASS_STRING);
  }

  void clear(Environment environment) {
      checkMutability(Token.self(), environment);
      clearImpl();
  }

    abstract void clearImpl();
    abstract boolean matches(SimiObjectImpl other, List<String> fieldsToMatch);
    abstract boolean contains(SimiValue object, Token at);
    abstract boolean isArray();
    abstract int length();

  void checkMutability(Token name, Environment environment) {
      if (this.immutable && environment.get(Token.self()).getObject() != this) {
          throw new RuntimeError(name, "Trying to alter an immutable object!");
      }
  }

  abstract ArrayList<SimiValue> keys();
  public abstract ArrayList<SimiValue> values();
  abstract SimiObjectImpl enumerate(SimiClassImpl objectClass);
  abstract SimiObjectImpl zip(SimiClassImpl objectClass);

  abstract SimiValue indexOf(SimiValue value);
  abstract SimiObjectImpl reversed();
  abstract Iterator<?> iterate();
  abstract SimiObjectImpl sorted(Comparator<?> comparator);

  boolean valuesMatch(SimiValue a, SimiValue b) {
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

    boolean valuesMatch(SimiProperty a, SimiProperty b) {
      if (a == null && b == null) {
          return true;
      }
      if (a == null || b == null) {
          return false;
      }
      return valuesMatch(a.value, b.value);
    }

  void append(SimiValue elem) {
      if (immutable) {
          throw new RuntimeException("Trying to append to an immutable object!");
      }
      appendImpl(elem);
  }
  abstract void appendImpl(SimiValue elem);

  abstract void addAll(SimiObjectImpl other);

  @Override
  public String toString() {
      SimiMethod method = clazz.findMethod(this, Constants.TO_STRING, 0);
      if (method != null && !method.function.isNative) {
          return method.call(Interpreter.sharedInstance, new ArrayList<>(), false).getString();
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

  abstract String printFields();

  abstract Dictionary asDictionary();
  abstract Array asArray();

    @Override
    public SimiClass getSimiClass() {
        return clazz;
    }

    @Override
    public SimiProperty get(String key, SimiEnvironment environment) {
        return get(Token.nativeCall(key), null, (Environment) environment);
    }

    @Override
    public void set(String key, SimiProperty prop, SimiEnvironment environment) {
      set(Token.nativeCall(key), prop, (Environment) environment);
    }

    static SimiObject getOrConvertObject(SimiValue value, Interpreter interpreter) {
        if (value == null || value instanceof SimiValue.Callable) {
            return null;
        }
      if (value instanceof SimiValue.Number || value instanceof SimiValue.String) {
          LinkedHashMap<String, SimiProperty> fields = new LinkedHashMap<>();
          fields.put(Constants.PRIVATE, new SimiProperty(value));
          return SimiObjectImpl.fromMap((SimiClassImpl) interpreter.getGlobal(
                    value instanceof SimiValue.Number ? Constants.CLASS_NUMBER : Constants.CLASS_STRING).value.getObject(),
                  true, fields);
      }
      return value.getObject();
    }

    static class Dictionary extends SimiObjectImpl {

        final LinkedHashMap<String, SimiProperty> fields;

        Dictionary(SimiClassImpl clazz,
                   boolean immutable,
                   LinkedHashMap<String, SimiProperty> fields) {
            super(clazz, immutable);
            this.fields = fields;
        }

        @Override
        SimiProperty get(Token name, Integer arity, Environment environment) {
            String key = name.lexeme;
            try {
                int index = Integer.parseInt(key);
                String implicitKey = Constants.IMPLICIT + index;
                if (fields.containsKey(implicitKey)) {
                    return bind(implicitKey, fields.get(implicitKey));
                } else {
                    if (clazz != null && clazz.name.equals(Constants.CLASS_STRING)) {
                        String value = fields.get(Constants.PRIVATE).value.getString();
                        return new SimiProperty(new SimiValue.String("" + value.charAt(index)));
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

            return getFromClass(name, arity);
        }

        @Override
        void setField(String key, SimiProperty prop) {
            if (prop == null) {
                fields.remove(key);
            } else {
                fields.put(key, prop);
            }
        }

        @Override
        void clearImpl() {
            fields.clear();
        }

        @Override
        boolean matches(SimiObjectImpl other, List<String> fieldsToMatch) {
            if (!(other instanceof Dictionary)) {
                return false;
            }
            Dictionary dictionary = (Dictionary) other;
            for (Map.Entry<String, SimiProperty> entry : fields.entrySet()) {
                if (fieldsToMatch != null && !fieldsToMatch.contains(entry.getKey())) {
                    continue;
                }
                SimiProperty prop = dictionary.fields.get(entry.getKey());
                if (!valuesMatch(prop, entry.getValue())) {
                    return false;
                }
            }
            return true;
        }

        @Override
        boolean contains(SimiValue object, Token at) {
            if (!(object instanceof SimiValue.String)) {
                throw new RuntimeError(at, "Left side must be a string!");
            }
            return fields.keySet().contains(object.getString());
        }

        @Override
        boolean isArray() {
            return false;
        }

        @Override
        int length() {
            return fields.size();
        }

        @Override
        ArrayList<SimiValue> keys() {
            return fields.keySet().stream().map(SimiValue.String::new).collect(Collectors.toCollection(ArrayList::new));
        }

        @Override
        public ArrayList<SimiValue> values() {
            return fields.values().stream().map(p -> p.value).collect(Collectors.toCollection(ArrayList::new));
        }

        @Override
        SimiObjectImpl enumerate(SimiClassImpl objectClass) {
            int size = length();
            ArrayList<SimiValue> values = new ArrayList<>(size);
            for (Map.Entry<String, SimiProperty> entry : fields.entrySet()) {
                values.add(new SimiValue.Object(SimiObjectImpl.decomposedPair(objectClass,
                        new SimiValue.String(entry.getKey()), entry.getValue().value)));
            }
            return SimiObjectImpl.fromArray(objectClass, true, values);
        }

        @Override
        SimiObjectImpl zip(SimiClassImpl objectClass) {
            if (length() == 0) {
                return new Dictionary(objectClass, true, new LinkedHashMap<>());
            }
            LinkedHashMap<String, SimiProperty> zipFields = new LinkedHashMap<>();
            for (SimiProperty prop : fields.values()) {
                Dictionary obj = ((SimiObjectImpl) prop.value.getObject()).asDictionary();
                zipFields.put(obj.fields.get(Constants.KEY).value.getString(), obj.fields.get(Constants.VALUE));
            }
            return new Dictionary(objectClass, true, zipFields);
        }

        @Override
        SimiValue indexOf(SimiValue value) {
            int index = fields.values().stream().map(p -> p.value).collect(Collectors.toList()).indexOf(value);
            if (index == -1) {
                return null;
            }
            return new SimiValue.Number(index);
        }

        @Override
        SimiObjectImpl reversed() {
            ListIterator<Map.Entry<String, SimiProperty>> iter =
                    new ArrayList<>(fields.entrySet()).listIterator(fields.size());
            LinkedHashMap<String, SimiProperty> reversedFields = new LinkedHashMap<>();
            while (iter.hasPrevious()) {
                Map.Entry<String, SimiProperty> entry = iter.previous();
                reversedFields.put(entry.getKey(), entry.getValue());
            }
            return new Dictionary(clazz, immutable, reversedFields);
        }

        @Override
        Iterator<?> iterate() {
            return fields.keySet().iterator();
        }

        @Override
        SimiObjectImpl sorted(Comparator<?> comparator) {
            LinkedHashMap<String, SimiProperty> sortedFields = new LinkedHashMap<>();
            fields.entrySet().stream()
                    .sorted((Comparator<? super Map.Entry<String, SimiProperty>>) comparator)
                    .forEach(e -> sortedFields.put(e.getKey(), e.getValue()));
            return SimiObjectImpl.fromMap(clazz, true, sortedFields);
        }

        @Override
        void appendImpl(SimiValue elem) {
            fields.put(Constants.IMPLICIT + fields.size(), new SimiProperty(elem));
        }

        @Override
        void addAll(SimiObjectImpl other) {
            if (other.isArray()) {
               throw new RuntimeException("Trying to add an array to object!");
            } else {
                for (Map.Entry<String, SimiProperty> entry : ((Dictionary) other).fields.entrySet()) {
                    if (!fields.containsKey(entry.getKey())) {
                        fields.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }

        @Override
        String printFields() {
            StringBuilder sb = new StringBuilder();
            for (String key : fields.keySet()) {
                sb.append("\t").append(key).append(" = ").append(fields.get(key)).append("\n");
            }
            return sb.toString();
        }

        @Override
        Dictionary asDictionary() {
            return this;
        }

        @Override
        Array asArray() {
            throw new IllegalArgumentException("Cannot cast Dictionary to Array!");
        }

        @Override
        public SimiObject clone(boolean mutable) {
            LinkedHashMap<String, SimiProperty> fieldsClone = new LinkedHashMap<>();
            for (Map.Entry<String, SimiProperty> entry : fields.entrySet()) {
                fieldsClone.put(entry.getKey(), entry.getValue().clone(mutable));
            }
            return new Dictionary(clazz, mutable, fieldsClone);
        }
    }

    static class Array extends SimiObjectImpl {

      final ArrayList<SimiValue> fields;

        Array(SimiClassImpl clazz,
                   boolean immutable,
                   ArrayList<SimiValue> fields) {
            super(clazz, immutable);
            this.fields = fields;
        }

        @Override
        SimiProperty get(Token name, Integer arity, Environment environment) {
            String key = name.lexeme;
            try {
                int index = Integer.parseInt(key);
                return bind(key, new SimiProperty(fields.get(index)));
            } catch (NumberFormatException ignored) { }
            return getFromClass(name, arity);
        }

        @Override
        void setField(String key, SimiProperty prop) {
            int index = Integer.parseInt(key);
            if (prop == null) {
                fields.remove(index);
            } else {
                fields.set(index, prop.value);
            }
        }

        @Override
        void clearImpl() {
            fields.clear();
        }

        @Override
        boolean matches(SimiObjectImpl other, List<String> fieldsToMatch) {
            if (!other.isArray()) {
                return false;
            }
            int length = length();
            if (other.length() != length) {
                return false;
            }
            for (int i = 0; i < length; i++) {
                if (!valuesMatch(fields.get(i), ((Array) other).fields.get(i))) {
                    return false;
                }
            }
            return true;
        }

        @Override
        boolean contains(SimiValue object, Token at) {
            return fields.contains(object);
        }

        @Override
        boolean isArray() {
            return true;
        }

        @Override
        int length() {
            return fields.size();
        }

        @Override
        ArrayList<SimiValue> keys() {
            return IntStream.range(0, length())
                    .mapToObj(SimiValue.Number::new)
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        @Override
        public ArrayList<SimiValue> values() {
            return new ArrayList<>(fields);
        }

        @Override
        SimiObjectImpl enumerate(SimiClassImpl objectClass) {
            int size = length();
            ArrayList<SimiValue> values = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                values.add(new SimiValue.Object(SimiObjectImpl.decomposedPair(objectClass, new SimiValue.Number(i), fields.get(i))));
            }
            return SimiObjectImpl.fromArray(objectClass, true, values);
        }

        @Override
        SimiObjectImpl zip(SimiClassImpl objectClass) {
            throw new RuntimeException("Can only zip arrays!");
        }

        @Override
        SimiValue indexOf(SimiValue value) {
            int index = fields.indexOf(value);
            if (index == -1) {
                return null;
            }
            return new SimiValue.Number(index);
        }

        @Override
        SimiObjectImpl reversed() {
            ArrayList<SimiValue> reversed = new ArrayList<>(fields);
            Collections.reverse(reversed);
            return new Array(clazz, immutable, reversed);
        }

        @Override
        Iterator<?> iterate() {
            return fields.iterator();
        }

        @Override
        SimiObjectImpl sorted(Comparator<?> comparator) {
            ArrayList<SimiValue> sorted = values();
            sorted.sort((Comparator<? super SimiValue>) comparator);
            return SimiObjectImpl.fromArray(clazz, true, sorted);
        }

        @Override
        void appendImpl(SimiValue elem) {
            fields.add(elem);
        }

        @Override
        void addAll(SimiObjectImpl other) {
            if (other.isArray()) {
                fields.addAll(((Array) other).fields);
            } else {
                throw new RuntimeException("Trying to add a dictionary to an array!");
            }
        }

        @Override
        String printFields() {
            StringBuilder sb = new StringBuilder();
            int size = length();
            for (int i = 0; i < size; i++) {
                sb.append("\t").append(i).append(" = ").append(fields.get(i)).append("\n");
            }
            return sb.toString();
        }

        @Override
        Dictionary asDictionary() {
            throw new IllegalArgumentException("Cannot cast Array to Dictionary!");
        }

        @Override
        Array asArray() {
            return this;
        }

        @Override
        public SimiObject clone(boolean mutable) {
            ArrayList<SimiValue> fieldsClone = new ArrayList<>();
            for (SimiValue field : fields) {
                fieldsClone.add(field.clone(mutable));
            }
            return new Array(clazz, mutable, fieldsClone);
        }
    }

    private static class InitiallyEmpty extends SimiObjectImpl {

        private SimiObjectImpl underlying;

        private InitiallyEmpty(SimiClassImpl clazz, boolean immutable) {
            super(clazz, immutable);
        }

        @Override
        SimiProperty get(Token name, Integer arity, Environment environment) {
            if (underlying == null) {
                return getFromClass(name, arity);
            }
            return underlying.get(name, arity, environment);
        }

        @Override
        void setField(String key, SimiProperty prop) {
            if (underlying == null) {
                LinkedHashMap<String, SimiProperty> fields = new LinkedHashMap<>();
                fields.put(key, prop);
                underlying = SimiObjectImpl.fromMap(clazz, immutable, fields);
            } else {
                underlying.setField(key, prop);
            }
        }

        @Override
        void clearImpl() {
            if (underlying != null) {
                underlying.clearImpl();
            }
        }

        @Override
        boolean matches(SimiObjectImpl other, List<String> fieldsToMatch) {
            return other.length() == 0;
        }

        @Override
        boolean contains(SimiValue object, Token at) {
            if (underlying == null) {
                return false;
            }
            return underlying.contains(object, at);
        }

        @Override
        boolean isArray() {
            if (underlying == null) {
                return false;
            }
            return underlying.isArray();
        }

        @Override
        int length() {
            if (underlying == null) {
                return 0;
            }
            return underlying.length();
        }

        @Override
        ArrayList<SimiValue> keys() {
            if (underlying == null) {
                return new ArrayList<>();
            }
            return underlying.keys();
        }

        @Override
        public ArrayList<SimiValue> values() {
            if (underlying == null) {
                return new ArrayList<>();
            }
            return underlying.values();
        }

        @Override
        SimiObjectImpl enumerate(SimiClassImpl objectClass) {
            if (underlying == null) {
                return SimiObjectImpl.fromArray(clazz, immutable, new ArrayList<>());
            }
            return underlying.enumerate(objectClass);
        }

        @Override
        SimiObjectImpl zip(SimiClassImpl objectClass) {
            if (underlying == null) {
                return SimiObjectImpl.fromMap(clazz, immutable, new LinkedHashMap<>());
            }
            return underlying.enumerate(objectClass);
        }

        @Override
        SimiValue indexOf(SimiValue value) {
            if (underlying == null) {
                return null;
            }
            return underlying.indexOf(value);
        }

        @Override
        SimiObjectImpl reversed() {
            if (underlying == null) {
                return SimiObjectImpl.empty(clazz, immutable);
            }
            return underlying.reversed();
        }

        @Override
        Iterator<?> iterate() {
            if (underlying == null) {
                return new ArrayList<SimiValue>().iterator();
            }
            return underlying.iterate();
        }

        @Override
        SimiObjectImpl sorted(Comparator<?> comparator) {
            if (underlying == null) {
                return SimiObjectImpl.empty(clazz, immutable);
            }
            return underlying.sorted(comparator);
        }

        @Override
        void appendImpl(SimiValue elem) {
            if (underlying == null) {
                ArrayList<SimiValue> fields = new ArrayList<>();
                fields.add(elem);
                underlying = SimiObjectImpl.fromArray(clazz, immutable, fields);
            } else {
                underlying.appendImpl(elem);
            }
        }

        @Override
        void addAll(SimiObjectImpl other) {
            if (underlying == null) {
                if (other.isArray()) {
                    underlying = SimiObjectImpl.fromArray(clazz, immutable, new ArrayList<>(underlying.values()));
                } else {
                    LinkedHashMap<String, SimiProperty> fields = new LinkedHashMap<>(other.asDictionary().fields);
                    underlying = SimiObjectImpl.fromMap(clazz, immutable, fields);
                }
            } else {
                underlying.addAll(other);
            }
        }

        @Override
        String printFields() {
            if (underlying == null) {
                return "";
            }
            return underlying.printFields();
        }

        @Override
        Dictionary asDictionary() {
            if (underlying == null) {
                return (Dictionary) SimiObjectImpl.fromMap(clazz, immutable, new LinkedHashMap<>());
            }
            return underlying.asDictionary();
        }

        @Override
        Array asArray() {
            if (underlying == null) {
                return (Array) SimiObjectImpl.fromArray(clazz, immutable, new ArrayList<>());
            }
            return underlying.asArray();
        }

        @Override
        public SimiObject clone(boolean mutable) {
            InitiallyEmpty clone = (InitiallyEmpty) SimiObjectImpl.empty(clazz, mutable);
            clone.underlying = (underlying != null) ? (SimiObjectImpl) underlying.clone(mutable) : null;
            return clone;
        }
    }
}
