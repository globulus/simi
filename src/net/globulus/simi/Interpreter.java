package net.globulus.simi;

import net.globulus.simi.api.*;

import java.util.ArrayList;
//< Functions import-array-list
//> Resolving and Binding import-hash-map
import java.util.HashMap;
//< Resolving and Binding import-hash-map
//> Statements and State import-list
import java.util.List;
//< Statements and State import-list
//> Resolving and Binding import-map
import java.util.Map;
class Interpreter implements SimiInterpreter, Expr.Visitor<Object>, Stmt.Visitor<Object> {

  final Environment globals = new Environment();
  private Environment environment = globals;
  private final Map<Expr, Integer> locals = new HashMap<>();

  Interpreter() {
    globals.define("clock", new SimiCallable() {
      @Override
      public int arity() {
        return 0;
      }

      @Override
      public Object call(SimiInterpreter interpreter,
                         List<Object> arguments,
                         boolean immutable) {
        return (double)System.currentTimeMillis() / 1000.0;
      }
    });
  }

  void interpret(List<Stmt> statements) {
    try {
      for (Stmt statement : statements) {
        execute(statement);
      }
    } catch (RuntimeError error) {
      Simi.runtimeError(error);
    }
  }

  private Object evaluate(Expr expr) {
    return expr.accept(this);
  }

  private void execute(Stmt stmt) {
    stmt.accept(this);
  }

  void resolve(Expr expr, int depth) {
    locals.put(expr, depth);
  }

  @Override
  public void executeBlock(SimiBlock block, SimiEnvironment environment) {
    Environment previous = this.environment;
    try {
      this.environment = (Environment) environment;

      for (SimiStatement statement : block.getStatements()) {
        execute((Stmt) statement);
      }
    } finally {
      this.environment = previous;
    }
  }

  @Override
  public Void visitBlockExpr(Expr.Block stmt) {
    executeBlock(stmt, new Environment(environment));
    return null;
  }

  @Override
  public Void visitClassStmt(Stmt.Class stmt) {
      environment.define(stmt.name.lexeme, null);
      List<SimiClassImpl> superclasses = null;
      if (stmt.superclasses != null) {
        superclasses = new ArrayList<>();
        for (Expr superclass : stmt.superclasses) {
            Object clazz = evaluate(superclass);
            if (!(clazz instanceof SimiClassImpl)) {
                throw new RuntimeError(stmt.name, "Superclass must be a class.");
            }
            superclasses.add((SimiClassImpl) clazz);
        }
      }
      environment = new Environment(environment);
      environment.define(Constants.SUPER, superclasses);

      Map<String, Value> constants = new HashMap<>();
      for (Expr.Assign constant : stmt.constants) {
          String key = constant.name.lexeme;
          Object value = evaluate(constant.value);
          constants.put(key, (Value) value);
      }

    Map<String, SimiFunction> methods = new HashMap<>();
    for (Stmt.Function method : stmt.methods) {
      SimiFunction function = new SimiFunction(method, environment,
          method.name.lexeme.equals(Constants.INIT), method.declaration.type == TokenType.NATIVE);
      methods.put(method.name.lexeme, function);
    }

    SimiClassImpl klass = new SimiClassImpl(stmt.name.lexeme, superclasses, constants, methods);

//    if (superclass != null) {
//      environment = environment.enclosing;
//    }

    environment.assign(stmt.name, klass);
    return null;
  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    evaluate(stmt.expression);
    return null; // [void]
  }

  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    SimiFunction function = new SimiFunction(stmt, environment, false, stmt.declaration.type == TokenType.NATIVE);
    environment.define(stmt.name.lexeme, function);
    return null;
  }

  @Override
  public Object visitElsifStmt(Stmt.Elsif stmt) {
    if (isTruthy(evaluate(stmt.condition))) {
      execute(stmt.thenBranch);
      return true;
    }
    return false;
  }

  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    if ((Boolean) visitElsifStmt(stmt.ifstmt)) {
      return null;
    }
    for (Stmt.Elsif elsif : stmt.elsifs) {
      if ((Boolean) visitElsifStmt(elsif)) {
        return null;
      }
    }
    if (stmt.elseBranch != null) {
      execute(stmt.elseBranch);
    }
    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    Object value = evaluate(stmt.expression);
    System.out.println(stringify(value));
    return null;
  }

  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    Object value = null;
    if (stmt.value != null) value = evaluate(stmt.value);

    throw new Return(value);
  }

  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    while (isTruthy(evaluate(stmt.condition))) {
      execute(stmt.body);
    }
    return null;
  }

  @Override
  public Void visitForStmt(Stmt.For stmt) {
    return null;
  }

    @Override
  public Object visitAssignExpr(Expr.Assign expr) {
    Object value = evaluate(expr.value);

    Integer distance = locals.get(expr);
    if (distance != null) {
      environment.assignAt(distance, expr.name, value);
    } else {
      globals.assign(expr.name, value);
    }

    return value;
  }

  @Override
  public Object visitBinaryExpr(Expr.Binary expr) {
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right); // [left]

    switch (expr.operator.type) {
      case BANG_EQUAL: return !isEqual(left, right);
      case EQUAL_EQUAL: return isEqual(left, right);
        case IS:
            return isInstance(left, right, expr);
        case ISNOT:
            return !isInstance(left, right, expr);
        case IN:
            return isIn(left, right, expr);
        case NOTIN:
            return !isIn(left, right, expr);
      case GREATER:
        checkNumberOperands(expr.operator, left, right);
        return (double)left > (double)right;
      case GREATER_EQUAL:
        checkNumberOperands(expr.operator, left, right);
        return (double)left >= (double)right;
      case LESS:
        checkNumberOperands(expr.operator, left, right);
        return (double)left < (double)right;
      case LESS_EQUAL:
        checkNumberOperands(expr.operator, left, right);
        return (double)left <= (double)right;
      case MINUS:
        checkNumberOperands(expr.operator, left, right);
        return (double)left - (double)right;
      case PLUS:
        if (left instanceof Double && right instanceof Double) {
          return (double)left + (double)right;
        } // [plus]

        if (left instanceof String && right instanceof String) {
          return left + (String)right;
        }

        throw new RuntimeError(expr.operator,
            "Operands must be two numbers or two strings.");
      case SLASH:
        checkNumberOperands(expr.operator, left, right);
        return (double)left / (double)right;
      case STAR:
        checkNumberOperands(expr.operator, left, right);
        return (double)left * (double)right;
        case MOD:
            checkNumberOperands(expr.operator, left, right);
            return (double)left % (double)right;
    }

    // Unreachable.
    return null;
  }

  @Override
  public Object visitCallExpr(Expr.Call expr) {
    Object callee = evaluate(expr.callee);

    List<Object> arguments = new ArrayList<>();
    for (Expr argument : expr.arguments) { // [in-order]
      arguments.add(evaluate(argument));
    }

    if (!(callee instanceof SimiCallable)) {
      throw new RuntimeError(expr.paren,
          "Can only call functions and classes.");
    }

    SimiCallable function = (SimiCallable)callee;
   if (arguments.size() != function.arity()) {
      throw new RuntimeError(expr.paren, "Expected " +
          function.arity() + " arguments but got " +
          arguments.size() + ".");
    }

    return function.call(this, arguments);
  }

  @Override
  public Object visitGetExpr(Expr.Get expr) {
    Object object = evaluate(expr.object);
    if (object instanceof SimiObjectImpl) {
      return ((SimiObjectImpl) object).get(expr.name, environment);
    }

    throw new RuntimeError(expr.name,
        "Only instances have properties.");
  }

  @Override
  public Object visitGroupingExpr(Expr.Grouping expr) {
    return evaluate(expr.expression);
  }

  @Override
  public Object visitLiteralExpr(Expr.Literal expr) {
    return expr.value;
  }

  @Override
  public Object visitLogicalExpr(Expr.Logical expr) {
    Object left = evaluate(expr.left);

    if (expr.operator.type == TokenType.OR) {
      if (isTruthy(left)) return left;
    } else {
      if (!isTruthy(left)) return left;
    }

    return evaluate(expr.right);
  }

  @Override
  public Object visitSetExpr(Expr.Set expr) {
    Object object = evaluate(expr.object);

    if (!(object instanceof SimiObjectImpl)) { // [order]
      throw new RuntimeError(expr.name, "Only objects have fields.");
    }

    Object value = evaluate(expr.value);
    ((SimiObjectImpl)object).set(expr.name, value, environment);
    return value;
  }

  @Override
  public Object visitSuperExpr(Expr.Super expr) {
    int distance = locals.get(expr);
    SimiClassImpl superclass = (SimiClassImpl)environment.getAt(
        distance, Constants.SUPER);

    // "self" is always one level nearer than "super"'s environment.
    SimiObjectImpl object = (SimiObjectImpl)environment.getAt(
        distance - 1, Constants.SELF);

    SimiFunction method = superclass.findMethod(
        object, expr.method.lexeme);

    if (method == null) {
      throw new RuntimeError(expr.method,
          "Undefined property '" + expr.method.lexeme + "'.");
    }

    return method;
  }

  @Override
  public Object visitSelfExpr(Expr.Self expr) {
    return lookUpVariable(expr.keyword, expr);
  }

  @Override
  public Object visitUnaryExpr(Expr.Unary expr) {
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
        case NOT:
        return !isTruthy(right);
      case MINUS:
        checkNumberOperand(expr.operator, right);
        return -(double)right;
    }
    // Unreachable.
    return null;
  }

  @Override
  public Object visitVariableExpr(Expr.Variable expr) {
    return lookUpVariable(expr.name, expr);
  }

    @Override
    public Object visitObjectLiteralExpr(Expr.ObjectLiteral expr) {
        return null;
    }

    private Object lookUpVariable(Token name, Expr expr) {
    Integer distance = locals.get(expr);
    if (distance != null) {
      return environment.getAt(distance, name.lexeme);
    } else {
      return globals.get(name);
    }
  }

  private void checkNumberOperand(Token operator, Object operand) {
    if (operand instanceof Double) return;
    throw new RuntimeError(operator, "Operand must be a number.");
  }

  private void checkNumberOperands(Token operator,
                                   Object left, Object right) {
    if (left instanceof Double && right instanceof Double) return;
    // [operand]
    throw new RuntimeError(operator, "Operands must be numbers.");
  }

  private boolean isTruthy(Object object) {
    if (object == null) return false;
    if (object instanceof Boolean) return (boolean)object;
    return true;
  }

  private boolean isEqual(Object a, Object b) {
    // nil is only equal to nil.
    if (a == null && b == null) return true;
    if (a == null) return false;

    return a.equals(b);
  }

  private boolean isInstance(Object a, Object b, Expr.Binary expr) {
    if (a == null || b == null) {
      return false;
    }
    if (!(a instanceof SimiObjectImpl)) {
      throw new RuntimeError(expr.operator, "Left side must be an Object!");
    }
    if (!(b instanceof SimiClassImpl)) {
        throw new RuntimeError(expr.operator, "Right side must be a Class!");
    }
    return ((SimiObjectImpl) a).is((SimiClassImpl) b);
  }

  private boolean isIn(Object a, Object b, Expr.Binary expr) {
      if (!(b instanceof SimiObjectImpl)) {
          throw new RuntimeError(expr.operator, "Right side must be an Object!");
      }
      return ((SimiObjectImpl) b).contains(a, expr.operator);
  }

  private String stringify(Object object) {
    if (object == null) return "nil";

    // Hack. Work around Java adding ".0" to integer-valued doubles.
    if (object instanceof Double) {
      String text = object.toString();
      if (text.endsWith(".0")) {
        text = text.substring(0, text.length() - 2);
      }
      return text;
    }

    return object.toString();
  }
}
