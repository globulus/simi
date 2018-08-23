package net.globulus.simi;

import java.util.*;

class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {

  private final Interpreter interpreter;
  Set<String> globalScope = new HashSet<>();
  private final Stack<Map<String, Boolean>> scopes = new Stack<>();
    private FunctionType currentFunction = FunctionType.NONE;

  Resolver(Interpreter interpreter) {
    this.interpreter = interpreter;
  }

  private enum FunctionType {
    NONE,
    FUNCTION,
    INITIALIZER,
    METHOD
  }
  private enum ClassType {
    NONE,
    CLASS,
    SUBCLASS
  }

  private ClassType currentClass = ClassType.NONE;

  void resolve(List<Stmt> statements) {
    for (Stmt statement : statements) {
      resolve(statement);
    }
  }

  @Override
  public Void visitBlockExpr(Expr.Block stmt, boolean newScope, boolean execute) {
    if (newScope) {
      beginScope();
    }
    resolve(stmt.statements);
    if (newScope) {
      endScope();
    }
    return null;
  }

  @Override
  public Void visitAnnotationStmt(Stmt.Annotation stmt) {
    return null;
  }

  @Override
  public Void visitBreakStmt(Stmt.Break stmt) {
    return null;
  }

  @Override
  public Void visitClassStmt(Stmt.Class stmt, boolean addToEnv) {
    declare(stmt.name, false);
    define(stmt.name);
    ClassType enclosingClass = currentClass;
    currentClass = ClassType.CLASS;
    boolean hasSuperclass = stmt.superclasses != null && !stmt.superclasses.isEmpty();
    if (hasSuperclass) {
      currentClass = ClassType.SUBCLASS;
      for (Expr superclass : stmt.superclasses) {
          resolve(superclass);
      }
      beginScope();
      scopes.peek().put(Constants.SUPER, true);
    }
    beginScope();
    scopes.peek().put(Constants.SELF, true);

    for (Stmt.Function method : stmt.methods) {
      FunctionType declaration = FunctionType.METHOD;
      if (method.name.lexeme.equals(Constants.INIT)) {
        declaration = FunctionType.INITIALIZER;
      }
      resolveFunction(method, declaration); // [local]
    }

    endScope();

    if (hasSuperclass) endScope();

    currentClass = enclosingClass;
    return null;
  }

  @Override
  public Void visitContinueStmt(Stmt.Continue stmt) {
    return null;
  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    resolve(stmt.expression);
    return null;
  }

  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    declare(stmt.name, false);
    define(stmt.name);
    resolveFunction(stmt, FunctionType.FUNCTION);
    return null;
  }

  @Override
  public Void visitElsifStmt(Stmt.Elsif stmt) {
      resolve(stmt.condition);
      resolve(stmt.thenBranch);
    return null;
  }

  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    visitElsifStmt(stmt.ifstmt);
    for (Stmt.Elsif elsif : stmt.elsifs) {
        visitElsifStmt(elsif);
    }
    if (stmt.elseBranch != null) resolve(stmt.elseBranch);
    return null;
  }

  @Override
  public Void visitImportStmt(Stmt.Import stmt) {
    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    resolve(stmt.expression);
    return null;
  }

  @Override
  public Void visitRescueStmt(Stmt.Rescue stmt) {
    resolve(stmt.block);
    return null;
  }

  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
//    if (currentFunction == FunctionType.NONE) {
//      Simi.error(stmt.keyword, "Cannot return from top-level code.");
//    }

    if (stmt.value != null) {
      if (currentFunction == FunctionType.INITIALIZER) {
        ErrorHub.sharedInstance().error(stmt.keyword, "Cannot return a value from an initializer.");
      }
      resolve(stmt.value);
    }

    return null;
  }

//  @Override
//  public Void visitVarStmt(Stmt.Var stmt) {
//    declare(stmt.name);
//    if (stmt.initializer != null) {
//      resolve(stmt.initializer);
//    }
//    define(stmt.name);
//    return null;
//  }

  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    resolve(stmt.condition);
    resolve(stmt.body);
    return null;
  }

  @Override
  public Void visitYieldStmt(Stmt.Yield stmt) {
    if (stmt.value != null) {
      if (currentFunction == FunctionType.INITIALIZER) {
        ErrorHub.sharedInstance().error(stmt.keyword, "Cannot yield a value from an initializer.");
      }
      resolve(stmt.value);
    }
    return null;
  }

  @Override
  public Void visitForStmt(Stmt.For stmt) {
    resolve(stmt.var);
    resolve(stmt.iterable);
    resolve(stmt.body);
    return null;
  }

  @Override
  public Void visitAnnotationsExpr(Expr.Annotations expr) {
    return null;
  }

  @Override
  public Void visitAssignExpr(Expr.Assign expr) {
    if (!declare(expr.name, true)) {
      ErrorHub.sharedInstance().error(expr.name, "Constant with this name already declared in this scope.");
    }
    resolve(expr.value);
    resolveLocal(expr, expr.name);
    return null;
  }

  @Override
  public Void visitBinaryExpr(Expr.Binary expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitCallExpr(Expr.Call expr) {
    resolve(expr.callee);

    for (Expr argument : expr.arguments) {
      resolve(argument);
    }

    return null;
  }

  @Override
  public Void visitGetExpr(Expr.Get expr) {
    resolve(expr.object);
    return null;
  }

  @Override
  public Void visitGroupingExpr(Expr.Grouping expr) {
    resolve(expr.expression);
    return null;
  }

  @Override
  public Void visitGuExpr(Expr.Gu expr) {
    return null;
  }

  @Override
  public Void visitIvicExpr(Expr.Ivic expr) {
    return null;
  }

  @Override
  public Void visitLiteralExpr(Expr.Literal expr) {
    return null;
  }

  @Override
  public Void visitLogicalExpr(Expr.Logical expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitSetExpr(Expr.Set expr) {
    resolve(expr.value);
    resolve(expr.object);
    return null;
  }

  @Override
  public Void visitSuperExpr(Expr.Super expr) {
//    if (currentClass == ClassType.NONE) {
//      Simi.error(expr.keyword,
//          "Cannot use 'super' outside of a class.");
//    } else if (currentClass != ClassType.SUBCLASS) {
//      Simi.error(expr.keyword,
//          "Cannot use 'super' in a class with no superclass.");
//    }

    resolveLocal(expr, expr.keyword);
    return null;
  }

  @Override
  public Void visitSelfExpr(Expr.Self expr) {
//    if (currentClass == ClassType.NONE) {
//      Simi.error(expr.keyword,
//          "Cannot use 'self' outside of a class.");
//      return null;
//    }

    resolveLocal(expr, expr.keyword);
    return null;
  }

  @Override
  public Void visitUnaryExpr(Expr.Unary expr) {
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitVariableExpr(Expr.Variable expr) {
    if (!scopes.isEmpty() &&
        scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
      ErrorHub.sharedInstance().error(expr.name,
          "Cannot read local variable in its own initializer.");
    }

    resolveLocal(expr, expr.name);
    return null;
  }

  @Override
  public Void visitObjectLiteralExpr(Expr.ObjectLiteral expr) {
//    resolve(expr);
    return null;
  }

  private void resolve(Stmt stmt) {
    stmt.accept(this);
  }

  private void resolve(Expr expr, Object... params) {
    expr.accept(this, params);
  }

  private void resolveFunctionBlock(Expr.Block block) {
      beginScope();
      for (Expr param : block.params) {
          Token name;
          if (param instanceof Expr.Variable) {
            name = ((Expr.Variable) param).name;
          } else {
            name = ((Expr.Variable) ((Expr.Binary) param).left).name;
          }
          declare(name, false);
          define(name);
      }
      resolve(block, false);
      endScope();
  }

  private void resolveFunction(
      Stmt.Function function, FunctionType type) {
    FunctionType enclosingFunction = currentFunction;
    currentFunction = type;
    resolveFunctionBlock(function.block);
    currentFunction = enclosingFunction;
  }

  private void beginScope() {
    scopes.push(new HashMap<>());
  }

  private void endScope() {
    scopes.pop();
  }

  private boolean declare(Token name, boolean autodefine) {
    String var = name.lexeme;
    boolean mutable = var.startsWith(Constants.MUTABLE);
    if (scopes.isEmpty()) {
      if (globalScope.contains(var)) {
        return mutable;
      }
      globalScope.add(var);
      return true;
    }
    for (Map<String, Boolean> scope : scopes) {
      if (scope.containsKey(var)) {
        return mutable;
      }
    }
    Map<String, Boolean> scope = scopes.peek();
    scope.put(var, autodefine);
    return true;
  }

  private void define(Token name) {
    if (scopes.isEmpty()) return;
    scopes.peek().put(name.lexeme, true);
  }

  private void resolveLocal(Expr expr, Token name) {
    if (globalScope.contains(name.lexeme)) {
      return;
    }
    for (int i = scopes.size() - 1; i >= 0; i--) {
      if (scopes.get(i).containsKey(name.lexeme)) {
        interpreter.resolve(expr, scopes.size() - 1 - i);
        return;
      }
    }
    // Not found. Assume it is global.
  }
}
