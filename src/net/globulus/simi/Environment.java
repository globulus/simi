package net.globulus.simi;

import net.globulus.simi.api.SimiEnvironment;
import net.globulus.simi.api.SimiProperty;
import net.globulus.simi.api.SimiValue;

import java.util.HashMap;
import java.util.Map;

class Environment implements SimiEnvironment {

  final Environment enclosing;
  private final Map<String, SimiProperty> props = new HashMap<>();
  private final Map<Stmt.BlockStmt, BlockImpl> statementBlocks = new HashMap<>();
  final int depth;

  Environment() {
    enclosing = null;
    depth = 0;
  }

  Environment(Environment enclosing) {
    this.enclosing = enclosing;
    depth = (enclosing != null) ? (enclosing.depth + 1) : 0;
  }

  boolean has(String key) {
    return props.containsKey(key);
  }

  SimiValue get(Token name) {
    if (props.containsKey(name.lexeme)) {
      return props.get(name.lexeme).getValue();
    }
    if (enclosing != null) {
      return enclosing.get(name);
    }
    return null;
//    throw new RuntimeError(name,
//        "Undefined variable '" + name.lexeme + "'.");
  }

  void assign(Token name, SimiProperty prop, boolean allowImmutable) {
      String key = name.lexeme;
      if (props.containsKey(key)) {
        if (allowImmutable) {
           props.put(key, prop);
        } else {
            throw new RuntimeError(name, "Cannot assign to a const, use " + TokenType.DOLLAR_EQUAL.toCode() + "!");
        }
      } else {
          define(key, prop);
      }

//    if (enclosing != null) {
//      enclosing.assign(name, value);
//      return;
//    }
//
//    throw new RuntimeError(name,
//        "Undefined variable '" + key + "'.");
  }

  @Override
  public void define(String name, SimiProperty property) {
    if (property == null || property.getValue() == null) {
      props.remove(name);
    } else {
      props.put(name, property);
    }
  }

  Environment ancestor(int distance) {
    Environment environment = this;
    for (int i = 0; i < distance; i++) {
      environment = environment.enclosing; // [coupled]
    }

    return environment;
  }

  SimiProperty getAt(int distance, String name) {
    return ancestor(distance).props.get(name);
  }

  void assignAt(int distance, Token name, SimiProperty prop) {
    assignAt(distance, name, prop, false);
  }

  void assignAt(int distance, Token name, SimiProperty prop, boolean allowImmutable) {
    ancestor(distance).assign(name, prop, allowImmutable);
  }

  @Override
  public SimiProperty tryGet(String name) {
    for (Environment env = this; env != null; env = env.enclosing) {
      SimiProperty prop = env.props.get(name);
      if (prop != null) {
        return prop;
      }
    }
    return null;
  }

  @Override
  public String toString() {
    String result = props.toString();
    if (enclosing != null) {
      result += " -> " + enclosing.toString();
    }
    return result;
  }

  String toStringWithoutValuesOrGlobal() {
    if (enclosing == null) {
      return "Global";
    }
    String result = props.keySet().toString();
    if (enclosing.enclosing != null) {
      result += " -> " + enclosing.toStringWithoutValuesOrGlobal();
    }
    return result;
  }

  BlockImpl getOrAssignBlock(Stmt.BlockStmt stmt,
                             Expr.Block declaration,
                             Map<Stmt.BlockStmt, SparseArray<BlockImpl>> yieldedStmts) {
    BlockImpl block = statementBlocks.get(stmt);
    if (block == null) {
      SparseArray<BlockImpl> yieldedBlocks = yieldedStmts.get(stmt);
      if (yieldedBlocks != null) {
        block = yieldedBlocks.get(depth);
      }
      if (block == null) {
        block = new BlockImpl(declaration, this);
        statementBlocks.put(stmt, block);
      }
    }
    return block;
  }

  void endBlock(Stmt.BlockStmt stmt, Map<Stmt.BlockStmt, SparseArray<BlockImpl>> yieldedStmts) {
    statementBlocks.remove(stmt);
    popBlock(stmt, yieldedStmts);
    for (Stmt.BlockStmt child : stmt.getChildren()) {
      endBlock(child, yieldedStmts);
    }
  }

  private void popBlock(Stmt.BlockStmt stmt, Map<Stmt.BlockStmt, SparseArray<BlockImpl>> yieldedStmts) {
    SparseArray<BlockImpl> blocks = yieldedStmts.get(stmt);
    if (blocks != null) {
      blocks.remove(this.depth);
    }
  }

  Environment deepClone() {
    Environment clone = new Environment(enclosing);
    for (Map.Entry<String, SimiProperty> entry : props.entrySet()) {
      SimiProperty value = entry.getValue();
      clone.props.put(entry.getKey(), (value == null) ? null : value.clone(false));
    }
    return clone;
  }
}
