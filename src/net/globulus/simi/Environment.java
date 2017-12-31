package net.globulus.simi;

import net.globulus.simi.api.SimiEnvironment;

import java.util.HashMap;
import java.util.Map;

class Environment implements SimiEnvironment {

  final Environment enclosing;
  private final Map<String, Object> values = new HashMap<>();

  Environment() {
    enclosing = null;
  }

  Environment(Environment enclosing) {
    this.enclosing = enclosing;
  }

  Object get(Token name) {
    if (values.containsKey(name.lexeme)) {
      return values.get(name.lexeme);
    }

    if (enclosing != null) return enclosing.get(name);

    throw new RuntimeError(name,
        "Undefined variable '" + name.lexeme + "'.");
  }

  void assign(Token name, Object value) {
      String key = name.lexeme;
    if (values.containsKey(key)) {
        if (key.startsWith(Constants.MUTABLE)) {
            values.put(key, value);
            return;
        } else {
            throw new RuntimeError(name, "Cannot assign to a const, use " + Constants.MUTABLE + " at the start of var name!");
        }
    } else {
        define(key, value);
    }

    if (enclosing != null) {
      enclosing.assign(name, value);
      return;
    }

    throw new RuntimeError(name,
        "Undefined variable '" + key + "'.");
  }

  void define(String name, Object value) {
    values.put(name, value);
  }

  Environment ancestor(int distance) {
    Environment environment = this;
    for (int i = 0; i < distance; i++) {
      environment = environment.enclosing; // [coupled]
    }

    return environment;
  }

  Object getAt(int distance, String name) {
    return ancestor(distance).values.get(name);
  }

  void assignAt(int distance, Token name, Object value) {
    ancestor(distance).assign(name, value);
  }

  @Override
  public String toString() {
    String result = values.toString();
    if (enclosing != null) {
      result += " -> " + enclosing.toString();
    }

    return result;
  }
}
