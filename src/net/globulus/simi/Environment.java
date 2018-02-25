package net.globulus.simi;

import net.globulus.simi.api.*;

import java.util.HashMap;
import java.util.List;
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
    depth = enclosing.depth + 1;
  }

  boolean has(String key) {
    return props.containsKey(key);
  }

  SimiValue get(Token name) {
    if (props.containsKey(name.lexeme)) {
      return props.get(name.lexeme).value;
    }
    if (enclosing != null) {
      return enclosing.get(name);
    }
    return null;
//    throw new RuntimeError(name,
//        "Undefined variable '" + name.lexeme + "'.");
  }

  void assign(Token name, SimiValue value, List<SimiObject> annotations, boolean allowImmutable) {
      String key = name.lexeme;
      SimiProperty prop = props.get(key);
      if (prop != null) {
        if (allowImmutable || key.startsWith(Constants.MUTABLE)) {
            prop.value = value;
        } else {
            throw new RuntimeError(name, "Cannot assign to a const, use " + Constants.MUTABLE + " at the start of var name!");
        }
      } else {
          define(key, value, annotations);
      }

//    if (enclosing != null) {
//      enclosing.assign(name, value);
//      return;
//    }
//
//    throw new RuntimeError(name,
//        "Undefined variable '" + key + "'.");
  }

  public void define(String name, SimiValue value) {
    define(name, value, null);
  }

  @Override
  public void define(String name, SimiValue value, List<SimiObject> annotations) {
    if (value == null) {
      props.remove(name);
    } else {
      props.put(name, new SimiProperty(value, annotations));
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

  void assignAt(int distance, Token name, SimiValue value, List<SimiObject> annotations) {
    ancestor(distance).assign(name, value, annotations, false);
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
