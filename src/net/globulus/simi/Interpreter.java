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
class Interpreter implements BlockInterpreter, Expr.Visitor<SimiValue>, Stmt.Visitor<Object> {

  final Environment globals = new Environment();
  private Environment environment = globals;
  private final Map<Expr, Integer> locals = new HashMap<>();

  Interpreter() {
    globals.define("clock", new SimiValue.Callable(new SimiCallable() {
      @Override
      public int arity() {
        return 0;
      }

      @Override
      public SimiValue call(BlockInterpreter interpreter,
                         List<SimiValue> arguments) {
        return new SimiValue.Number((double)System.currentTimeMillis() / 1000.0);
      }
    }));
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

  private SimiValue evaluate(Expr expr) {
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
  public SimiValue visitBlockExpr(Expr.Block stmt, boolean newScope) {
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
            SimiObject clazz = evaluate(superclass).getObject();
            if (!(clazz instanceof SimiClassImpl)) {
                throw new RuntimeError(stmt.name, "Superclass must be a class.");
            }
            superclasses.add((SimiClassImpl) clazz);
        }
      }
      environment = new Environment(environment);
      environment.define(Constants.SUPER, new SimiClassImpl.SuperClassesList(superclasses));

      Map<String, SimiValue> constants = new HashMap<>();
      for (Expr.Assign constant : stmt.constants) {
          String key = constant.name.lexeme;
          SimiValue value = evaluate(constant.value);
          constants.put(key, value);
      }

    Map<OverloadableFunction, SimiFunction> methods = new HashMap<>();
    for (Stmt.Function method : stmt.methods) {
        String name = method.name.lexeme;
      SimiFunction function = new SimiFunction(method, environment,
          name.equals(Constants.INIT), method.declaration.type == TokenType.NATIVE);
      methods.put(new OverloadableFunction(name, function.arity()), function);
    }

    SimiClassImpl klass = new SimiClassImpl(stmt.name.lexeme, superclasses, constants, methods);

//    if (superclass != null) {
//      environment = environment.enclosing;
//    }

    environment.assign(stmt.name, new SimiValue.Object(klass), false);
    return null;
  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    evaluate(stmt.expression);
    return null; // [void]
  }

  @Override
  public SimiValue visitFunctionStmt(Stmt.Function stmt) {
    SimiFunction function = new SimiFunction(stmt, environment, false, stmt.declaration.type == TokenType.NATIVE);
    SimiValue value = new SimiValue.Callable(function);
    environment.define(stmt.name.lexeme, value);
    return value;
  }

  @Override
  public Object visitElsifStmt(Stmt.Elsif stmt) {
    if (isTruthy(evaluate(stmt.condition))) {
      evaluate(stmt.thenBranch);
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
      evaluate(stmt.elseBranch);
    }
    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    SimiValue value = evaluate(stmt.expression);
    System.out.println(stringify(value));
    return null;
  }

  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    SimiValue value = null;
    if (stmt.value != null) {
      value = evaluate(stmt.value);
    }

    throw new Return(value);
  }

  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    while (isTruthy(evaluate(stmt.condition))) {
      evaluate(stmt.body);
    }
    return null;
  }

  @Override
  public Void visitForStmt(Stmt.For stmt) {
    List<Expr> emptyArgs = new ArrayList<>();
    SimiObjectImpl iterable = (SimiObjectImpl) evaluate(stmt.iterable).getObject();
    Token iterateToken = new Token(TokenType.IDENTIFIER, Constants.ITERATE, null, stmt.var.name.line);
    SimiObjectImpl iterator = (SimiObjectImpl) call(iterable.get(iterateToken, 0, environment), iterateToken, emptyArgs).getObject();
    Token nextToken = new Token(TokenType.IDENTIFIER, Constants.NEXT, null, stmt.var.name.line);
    SimiValue nextMethod = iterator.get(nextToken, 0, environment);
    while (true) {
      SimiValue var = call(nextMethod, nextToken, emptyArgs);
      if (var == null) {
        break;
      }
      environment.assign(stmt.var.name, var, true);
      evaluate(stmt.body);
    }
    return null;
  }

    @Override
  public SimiValue visitAssignExpr(Expr.Assign expr) {
    SimiValue value;
    if (expr.value instanceof Expr.Block) {
      value = visitFunctionStmt(new Stmt.Function(expr.name, expr.name, (Expr.Block) expr.value));
    } else {
      value = evaluate(expr.value);
    }
    Integer distance = locals.get(expr);
    if (distance != null) {
      environment.assignAt(distance, expr.name, value);
    } else {
      globals.assign(expr.name, value, false);
    }
    return value;
  }

  @Override
  public SimiValue visitBinaryExpr(Expr.Binary expr) {
    SimiValue left = evaluate(expr.left);
    SimiValue right = evaluate(expr.right); // [left]

    switch (expr.operator.type) {
      case BANG_EQUAL: return new SimiValue.Number(!isEqual(left, right));
      case EQUAL_EQUAL: return new SimiValue.Number(isEqual(left, right));
        case IS:
            return new SimiValue.Number(isInstance(left, right, expr));
        case ISNOT:
            return new SimiValue.Number(!isInstance(left, right, expr));
        case IN:
            return new SimiValue.Number(isIn(left, right, expr));
        case NOTIN:
            return new SimiValue.Number(!isIn(left, right, expr));
      case GREATER:
        checkNumberOperands(expr.operator, left, right);
        return new SimiValue.Number(left.getNumber() > right.getNumber());
      case GREATER_EQUAL:
        checkNumberOperands(expr.operator, left, right);
          return new SimiValue.Number(left.getNumber() >= right.getNumber());
      case LESS:
        checkNumberOperands(expr.operator, left, right);
          return new SimiValue.Number(left.getNumber() < right.getNumber());
      case LESS_EQUAL:
        checkNumberOperands(expr.operator, left, right);
          return new SimiValue.Number(left.getNumber() <= right.getNumber());
      case MINUS:
        checkNumberOperands(expr.operator, left, right);
          return new SimiValue.Number(left.getNumber() - right.getNumber());
      case PLUS:
        if (left instanceof SimiValue.Number && right instanceof SimiValue.Number) {
            return new SimiValue.Number(left.getNumber() + right.getNumber());
        } // [plus]

        if (left instanceof SimiValue.String && right instanceof SimiValue.String) {
            return new SimiValue.String(left.getString() + right.getString());
        }

        throw new RuntimeError(expr.operator,
            "Operands must be two numbers or two strings.");
      case SLASH:
        checkNumberOperands(expr.operator, left, right);
          return new SimiValue.Number(left.getNumber() / right.getNumber());
      case STAR:
        checkNumberOperands(expr.operator, left, right);
          return new SimiValue.Number(left.getNumber() * right.getNumber());
        case MOD:
            checkNumberOperands(expr.operator, left, right);
            return new SimiValue.Number(left.getNumber() % right.getNumber());
    }

    // Unreachable.
    return null;
  }

  @Override
  public SimiValue visitCallExpr(Expr.Call expr) {
    SimiValue callee = evaluate(expr.callee);
    return call(callee, expr.paren, expr.arguments);
  }

  private SimiValue call(SimiValue callee, Token paren, List<Expr> args) {
    List<SimiValue> arguments = new ArrayList<>();
    for (Expr argument : args) { // [in-order]
      arguments.add(evaluate(argument));
    }

    SimiCallable function;
    if (callee instanceof SimiValue.Object) {
      SimiObject value = callee.getObject();
      if (!(value instanceof SimiClassImpl)) {
        throw new RuntimeError(paren,"Can only call functions and classes.");
      }
      return ((SimiClassImpl) value).init(this, arguments);
    } else if (callee instanceof SimiValue.Callable) {
      function = callee.getCallable();
    } else {
      throw new RuntimeError(paren,"Can only call functions and classes.");
    }

    if (arguments.size() != function.arity()) {
      throw new RuntimeError(paren, "Expected " +
              function.arity() + " arguments but got " +
              arguments.size() + ".");
    }
    return function.call(this, arguments);
  }

  @Override
  public SimiValue visitGetExpr(Expr.Get expr) {
    SimiValue object = evaluate(expr.object);
    try {
        return ((SimiObjectImpl) object.getObject()).get(expr.name, expr.arity, environment);
    } catch (SimiValue.IncompatibleValuesException e) {
        throw new RuntimeError(expr.name,"Only instances have properties.");
    }
  }

  @Override
  public SimiValue visitGroupingExpr(Expr.Grouping expr) {
    return evaluate(expr.expression);
  }

  @Override
  public SimiValue visitLiteralExpr(Expr.Literal expr) {
    return expr.value;
  }

  @Override
  public SimiValue visitLogicalExpr(Expr.Logical expr) {
    SimiValue left = evaluate(expr.left);

    if (expr.operator.type == TokenType.OR) {
      if (isTruthy(left)) return left;
    } else {
      if (!isTruthy(left)) return left;
    }

    return evaluate(expr.right);
  }

  @Override
  public SimiValue visitSetExpr(Expr.Set expr) {
    SimiValue object = evaluate(expr.object);

    if (!(object instanceof SimiValue.Object)) { // [order]
      throw new RuntimeError(expr.name, "Only objects have fields.");
    }

    SimiValue value = evaluate(expr.value);
    ((SimiObjectImpl) object.getObject()).set(expr.name, value, environment);
    return value;
  }

  @Override
  public SimiValue visitSuperExpr(Expr.Super expr) {
    int distance = locals.get(expr);
    SimiClassImpl superclass = (SimiClassImpl) environment.getAt(distance, Constants.SUPER).getObject();

    // "self" is always one level nearer than "super"'s environment.
    SimiObjectImpl object = (SimiObjectImpl) environment.getAt(distance - 1, Constants.SELF).getObject();

    SimiFunction method = superclass.findMethod(object, expr.method.lexeme, expr.arity);

    if (method == null) {
      throw new RuntimeError(expr.method,
          "Undefined property '" + expr.method.lexeme + "'.");
    }

    return new SimiValue.Callable(method);
  }

  @Override
  public SimiValue visitSelfExpr(Expr.Self expr) {
    return lookUpVariable(expr.keyword, expr);
  }

  @Override
  public SimiValue visitUnaryExpr(Expr.Unary expr) {
    SimiValue right = evaluate(expr.right);

    switch (expr.operator.type) {
        case NOT:
        return new SimiValue.Number(!isTruthy(right));
      case MINUS:
        checkNumberOperand(expr.operator, right);
        return new SimiValue.Number(-right.getNumber());
    }
    // Unreachable.
    return null;
  }

  @Override
  public SimiValue visitVariableExpr(Expr.Variable expr) {
    return lookUpVariable(expr.name, expr);
  }

    @Override
    public SimiValue visitObjectLiteralExpr(Expr.ObjectLiteral expr) {
        return null;
    }

    private SimiValue lookUpVariable(Token name, Expr expr) {
        Integer distance = locals.get(expr);
        if (distance != null) {
          return environment.getAt(distance, name.lexeme);
        } else {
          SimiValue value = environment.tryGet(name.lexeme);
          if (value == null) {
            return globals.get(name);
          }
          return value;
        }
    }

  private void checkNumberOperand(Token operator, SimiValue operand) {
    if (operand instanceof SimiValue.Number) {
      return;
    }
    throw new RuntimeError(operator, "Operand must be a number.");
  }

  private void checkNumberOperands(Token operator, SimiValue left, SimiValue right) {
    if (left instanceof SimiValue.Number && right instanceof SimiValue.Number) {
      return;
    }
    throw new RuntimeError(operator, "Operands must be numbers.");
  }

  private boolean isTruthy(SimiValue object) {
    if (object == null) {
        return false;
    }
    try {
        double value = object.getNumber();
        return value != 0;
    } catch (SimiValue.IncompatibleValuesException e) {
        return true;
    }
  }

  private boolean isEqual(SimiValue a, SimiValue b) {
    // nil is only equal to nil.
    if (a == null && b == null) {
      return true;
    }
    if (a == null) {
      return false;
    }
    return a.equals(b);
  }

  private boolean isInstance(SimiValue a, SimiValue b, Expr.Binary expr) {
    if (a == null || b == null) {
      return false;
    }
    if (!(a instanceof SimiValue.Object)) {
      throw new RuntimeError(expr.operator, "Left side must be an Object!");
    }
    if (!(b instanceof SimiValue.Object)) {
        throw new RuntimeError(expr.operator, "Right side must be a Class!");
    }
    return ((SimiObjectImpl) a.getObject()).is((SimiClassImpl) b.getObject());
  }

  private boolean isIn(SimiValue a, SimiValue b, Expr.Binary expr) {
      if (!(b instanceof SimiValue.Object)) {
          throw new RuntimeError(expr.operator, "Right side must be an Object!");
      }
      return ((SimiObjectImpl) b.getObject()).contains(a, expr.operator);
  }

  private String stringify(SimiValue object) {
    if (object == null) {
      return "nil";
    }
    return object.toString();
  }
}
