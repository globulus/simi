package net.globulus.simi;

import net.globulus.simi.api.*;

import java.util.HashMap;
import java.util.Map;

class Environment implements SimiEnvironment, ValueStorage {

  private final Environment enclosing;
//  private final Map<String, SimiValue> values = new HashMap<>();
  private final ValueStorageImpl values = new ValueStorageImpl();
  private final Map<Stmt.BlockStmt, BlockImpl> statementBlocks = new HashMap<>();
  final int depth;

  Environment() {
    enclosing = null;
    depth = 0;
  }

  Environment(Environment enclosing) {
    this.enclosing = enclosing;
    depth = enclosing.depth + 1;
  }

//  SimiValue get(Token name) {
//    if (values.containsKey(name.lexeme)) {
//      return values.get(name.lexeme);
//    }
//    if (enclosing != null) {
//      return enclosing.get(name);
//    }
//    return null;
//    throw new RuntimeError(name,
//        "Undefined variable '" + name.lexeme + "'.");
//  }

  void assign(Token name, Object value, boolean allowImmutable) {
      String key = name.lexeme;
      if (get(key) != null) {
        if (allowImmutable || key.startsWith(Constants.MUTABLE)) {
            put(key, value);
        } else {
            throw new RuntimeError(name, "Cannot assign to a const, use " + Constants.MUTABLE + " at the start of var name!");
        }
      } else {
        put(key, value);
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
  public void define(String name, SimiValue value) {
    put(name, value);
  }

  Environment ancestor(int distance) {
    Environment environment = this;
    for (int i = 0; i < distance; i++) {
      environment = environment.enclosing; // [coupled]
    }

    return environment;
  }

  Object getAt(int distance, String name) {
    return ancestor(distance).get(name);
  }

  void assignAt(int distance, Token name, Object value) {
    ancestor(distance).assign(name, value, false);
  }

//  Environment trimmedSelf() {
//    if (enclosing == null) {
//      return this;
//    }
//    Environment environment = new Environment(enclosing);
//    for (Map.Entry<String, SimiValue> entry : values.entrySet()) {
//      boolean put = false;
//      if (entry.getValue() instanceof SimiValue.Callable) {
//        put = true;
//      } else if (entry.getValue() instanceof SimiValue.Object) {
//        SimiObject object = entry.getValue().getObject();
//        if (object instanceof SimiClass) {
//          put = true;
//        }
//      }
//      if (put) {
//        environment.values.put(entry.getKey(), entry.getValue());
//      }
//    }
//    return environment;
//  }

  public Object tryGetObject(String name) {
    for (Environment env = this; env != null; env = env.enclosing) {
      Object value = env.get(name);
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  @Override
  public SimiValue tryGet(String name) {
    for (Environment env = this; env != null; env = env.enclosing) {
      SimiValue value = env.getValue(name);
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  @Override
  public String toString() {
    String result = values.toString();
    if (enclosing != null) {
      result += " -> " + enclosing.toString();
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

  @Override
  public void put(String name, Double value) {
    values.put(name, value);
  }

  @Override
  public void put(String name, String value) {
    values.put(name, value);
  }

  @Override
  public void put(String name, SimiObject value) {
    values.put(name, value);
  }

  @Override
  public void put(String name, SimiCallable value) {
    values.put(name, value);
  }

  @Override
  public void put(String name, SimiValue value) {
    values.put(name, value);
  }

  @Override
  public void put(String name, Object value) {
    values.put(name, value);
  }

  @Override
  public Double getNumber(String name) {
    Double value = values.getNumber(name);
    if (value == null && enclosing != null) {
      return enclosing.getNumber(name);
    }
    return value;
  }

  @Override
  public String getString(String name) {
    String value = values.getString(name);
    if (value == null && enclosing != null) {
      return enclosing.getString(name);
    }
    return value;
  }

  @Override
  public SimiObject getObject(String name) {
    SimiObject value = values.getObject(name);
    if (value == null && enclosing != null) {
      return enclosing.getObject(name);
    }
    return value;
  }

  @Override
  public SimiCallable getCallable(String name) {
    SimiCallable value = values.getCallable(name);
    if (value == null && enclosing != null) {
      return enclosing.getCallable(name);
    }
    return value;
  }

  @Override
  public SimiValue getValue(String name) {
    SimiValue value = values.getValue(name);
    if (value == null && enclosing != null) {
      return enclosing.getValue(name);
    }
    return value;
  }

  @Override
  public Object get(String name) {
    Double value = values.getNumber(name);
    if (value == null && enclosing != null) {
      return enclosing.getNumber(name);
    }
    return value;
  }

  @Override
  public boolean remove(String name) {
    return values.remove(name) || enclosing == null || enclosing.remove(name);
  }
}
