package net.globulus.simi;

import net.globulus.simi.api.SimiClass;
import net.globulus.simi.api.SimiEnvironment;
import net.globulus.simi.api.SimiObject;
import net.globulus.simi.api.SimiValue;

import java.util.HashMap;
import java.util.Map;

class Environment implements SimiEnvironment {

  final Environment enclosing;
  private final Map<String, SimiValue> values = new HashMap<>();
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

  boolean has(String key) {
    return values.containsKey(key);
  }

  SimiValue get(Token name) {
    if (values.containsKey(name.lexeme)) {
      return values.get(name.lexeme);
    }
    if (enclosing != null) {
      return enclosing.get(name);
    }
    return null;
//    throw new RuntimeError(name,
//        "Undefined variable '" + name.lexeme + "'.");
  }

  void assign(Token name, SimiValue value, boolean allowImmutable) {
      String key = name.lexeme;
      if (values.get(key) != null) {
        if (allowImmutable || key.startsWith(Constants.MUTABLE)) {
            values.put(key, value);
        } else {
            throw new RuntimeError(name, "Cannot assign to a const, use " + Constants.MUTABLE + " at the start of var name!");
        }
      } else {
          define(key, value);
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
    values.put(name, value);
  }

  Environment ancestor(int distance) {
    Environment environment = this;
    for (int i = 0; i < distance; i++) {
      environment = environment.enclosing; // [coupled]
    }

    return environment;
  }

  SimiValue getAt(int distance, String name) {
    return ancestor(distance).values.get(name);
  }

  void assignAt(int distance, Token name, SimiValue value) {
    ancestor(distance).assign(name, value, false);
  }

  Environment trimmedSelf() {
    if (enclosing == null) {
      return this;
    }
    Environment environment = new Environment(enclosing);
    for (Map.Entry<String, SimiValue> entry : values.entrySet()) {
      boolean put = false;
      if (entry.getValue() instanceof SimiValue.Callable) {
        put = true;
      } else if (entry.getValue() instanceof SimiValue.Object) {
        SimiObject object = entry.getValue().getObject();
        if (object instanceof SimiClass) {
          put = true;
        }
      }
      if (put) {
        environment.values.put(entry.getKey(), entry.getValue());
      }
    }
    return environment;
  }

  @Override
  public SimiValue tryGet(String name) {
    for (Environment env = this; env != null; env = env.enclosing) {
      SimiValue value = env.values.get(name);
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
}
