package net.globulus.simi;

import net.globulus.simi.api.*;

import java.util.*;
import java.util.stream.Collectors;

class Interpreter implements BlockInterpreter, Expr.Visitor<Object>, Stmt.Visitor<Object> {

  final NativeModulesManager nativeModulesManager;
  private final Environment globals = new Environment();
  private Environment environment = globals;
  private final Map<Expr, Integer> locals = new HashMap<>();
  private final BaseClassesNativeImpl baseClassesNativeImpl = new BaseClassesNativeImpl();
  private final Stack<SimiBlock> loopBlocks = new Stack<>();
  private final Stack<SimiException> raisedExceptions = new Stack<>();
  private final Map<Stmt.BlockStmt, SparseArray<BlockImpl>> yieldedStmts = new HashMap<>();

  static Interpreter sharedInstance;

  Interpreter(NativeModulesManager nativeModulesManager) {
    sharedInstance = this;
    this.nativeModulesManager = nativeModulesManager;
    globals.define("clock", new SimiValue.Callable(new SimiCallable() {
      @Override
      public int arity() {
        return 0;
      }

      @Override
      public SimiValue call(BlockInterpreter interpreter,
                          List<SimiValue> arguments,
                            boolean rethrow) {
        return new SimiValue.Number((double)System.currentTimeMillis() / 1000.0);
      }
    }, "clock", null));
  }

  void interpret(List<Stmt> statements) {
    try {
      for (Stmt statement : statements) {
        if (raisedExceptions.isEmpty()) {
          execute(statement);
        } else {
          throw raisedExceptions.peek();
        }
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
  public void executeBlock(SimiBlock block, SimiEnvironment environment, int startAt) {
    Environment previous = this.environment;
    try {
      this.environment = (Environment) environment;
      List<? extends SimiStatement> statements = block.getStatements();
      int size = statements.size();
      for (int i = startAt < size ? startAt : 0; i < size; i++) {
        try {
          if (raisedExceptions.isEmpty()) {
            Stmt statement = (Stmt) statements.get(i);
            execute(statement);
          } else {
            Stmt.Rescue rescue = null;
            for (; i < size; i++) {
              Stmt statement = (Stmt) statements.get(i);
              if (statement instanceof Stmt.Rescue) {
                rescue = (Stmt.Rescue) statement;
                break;
              }
            }
            if (rescue != null) {
              SimiException e = raisedExceptions.pop();
              executeRescueBlock(rescue, e);
            }
          }
        } catch (Yield yield) {
          block.yield(i + (yield.rethrown ? 0 : 1));
          throw yield;
        }
      }
    } finally {
      this.environment = previous;
    }
  }

  @Override
  public SimiValue getGlobal(String name) {
    return globals.getValue(name);
  }

  @Override
  public SimiEnvironment getEnvironment() {
    return environment;
  }

  @Override
  public void raiseException(SimiException e) {
    raisedExceptions.push(e);
  }

  @Override
  public SimiObject newObject(boolean immutable, LinkedHashMap<String, SimiValue> props) {
    return SimiObjectImpl.fromMap(getObjectClass(), immutable, props);
  }

  @Override
  public SimiObject newArray(boolean immutable, ArrayList<SimiValue> props) {
    return SimiObjectImpl.fromArray(getObjectClass(), immutable, props);
  }

  @Override
  public SimiObject newInstance(SimiClass clazz, LinkedHashMap<String, SimiValue> props) {
    return SimiObjectImpl.instance((SimiClassImpl) clazz, props);
  }

  @Override
  public SimiValue visitBlockExpr(Expr.Block stmt, boolean newScope) {
    executeBlock(stmt, new Environment(environment), 0);
    return null;
  }

  @Override
  public Void visitBreakStmt(Stmt.Break stmt) {
    if (loopBlocks.isEmpty()) {
      Simi.error(stmt.name, "Break outside a loop!");
    }
    throw new Break();
  }

  @Override
  public Void visitClassStmt(Stmt.Class stmt) {
      String className = stmt.name.lexeme;
      boolean isBaseClass = isBaseClass(className);
      if (isBaseClass) {
          globals.define(className, null);
      } else {
          environment.define(className, null);
      }
      List<SimiClassImpl> superclasses = null;
      if (stmt.superclasses != null) {
        superclasses = new ArrayList<>();
        for (Expr superclass : stmt.superclasses) {
            SimiObject clazz = (SimiObject) evaluate(superclass);
            if (!(clazz instanceof SimiClassImpl)) {
                throw new RuntimeError(stmt.name, "Superclass must be a class.");
            }
            superclasses.add((SimiClassImpl) clazz);
        }
      } else if (!isBaseClass) {
          superclasses = Collections.singletonList(getObjectClass());
      }
      environment = new Environment(environment);
      environment.define(Constants.SUPER, new SimiClassImpl.SuperClassesList(superclasses));

      Map<String, SimiValue> constants = new HashMap<>();
      for (Expr.Assign constant : stmt.constants) {
          String key = constant.name.lexeme;
          SimiValue value = (SimiValue) evaluate(constant.value);
          constants.put(key, value);
      }

    Map<OverloadableFunction, SimiFunction> methods = new HashMap<>();
    for (Stmt.Function method : stmt.methods) {
        String name = method.name.lexeme;
      SimiFunction function = new SimiFunction(method, environment,
          name.equals(Constants.INIT), method.block.isNative());
      methods.put(new OverloadableFunction(name, function.arity()), function);
    }

    SimiClassImpl klass = new SimiClassImpl(className, superclasses, constants, methods);

//    if (superclass != null) {
//      environment = environment.enclosing;
//    }

    if (isBaseClass) {
        globals.assign(stmt.name, new SimiValue.Object(klass), false);
    } else {
        environment.assign(stmt.name, new SimiValue.Object(klass), false);
    }
    return null;
  }

  @Override
  public Void visitContinueStmt(Stmt.Continue stmt) {
    if (loopBlocks.isEmpty()) {
      Simi.error(stmt.name, "Continue outside a loop!");
    }
    throw new Continue();
  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    evaluate(stmt.expression);
    return null; // [void]
  }

  @Override
  public SimiValue visitFunctionStmt(Stmt.Function stmt) {
    SimiFunction function = new SimiFunction(stmt, environment, false, stmt.block.isNative());
    SimiValue value = new SimiValue.Callable(function, stmt.name.lexeme, null);
    environment.define(stmt.name.lexeme, value);
    return value;
  }

  @Override
  public Object visitElsifStmt(Stmt.Elsif stmt) {
    if (isTruthy(evaluate(stmt.condition))) {
      BlockImpl block = this.environment.getOrAssignBlock(stmt, stmt.thenBranch, yieldedStmts);
      try {
        block.call(this, null, true);
      } catch (Return | Yield returnYield) {
        if (returnYield instanceof Return) {
          this.environment.endBlock(stmt, yieldedStmts);
        } else {
          putBlock(stmt, block);
        }
        throw returnYield;
      }
      this.environment.endBlock(stmt, yieldedStmts);
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
      BlockImpl elseBlock = this.environment.getOrAssignBlock(stmt, stmt.elseBranch, yieldedStmts);
      try {
        elseBlock.call(this, null, true);
      } catch (Return | Yield returnYield) {
        if (returnYield instanceof Return) {
          this.environment.endBlock(stmt, yieldedStmts);
        } else {
          putBlock(stmt, elseBlock);
        }
        throw returnYield;
      }
      this.environment.endBlock(stmt, yieldedStmts);
    }
    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    SimiValue value = (SimiValue) evaluate(stmt.expression);
    System.out.println(stringify(value));
    return null;
  }

  @Override
  public Void visitRescueStmt(Stmt.Rescue stmt) {
    executeRescueBlock(stmt, null);
    return null;
  }

  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    Object value = null;
    if (stmt.value != null) {
      value = evaluate(stmt.value);
    }
    throw new Return(value);
  }

  @Override
  public Object visitYieldStmt(Stmt.Yield stmt) {
    Object value = null;
    if (stmt.value != null) {
      value = evaluate(stmt.value);
    }
    throw new Yield(value, false);
  }

  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    loopBlocks.push(stmt.body);
    BlockImpl block = this.environment.getOrAssignBlock(stmt, stmt.body, yieldedStmts);
    while (isTruthy(evaluate(stmt.condition))) {
      try {
        block.call(this, null, true);
      } catch (Return | Yield returnYield) {
          if (returnYield instanceof Return) {
            this.environment.endBlock(stmt, yieldedStmts);
          } else {
            putBlock(stmt, block);
          }
          loopBlocks.pop();
          throw returnYield;
      } catch (Break b) {
        break;
      } catch (Continue ignored) { }
    }
    this.environment.endBlock(stmt, yieldedStmts);
    loopBlocks.pop();
    return null;
  }

  @Override
  public Void visitForStmt(Stmt.For stmt) {
    BlockImpl block = this.environment.getOrAssignBlock(stmt, stmt.body, yieldedStmts);

    List<Expr> emptyArgs = new ArrayList<>();
    Token nextToken = new Token(TokenType.IDENTIFIER, Constants.NEXT, null, stmt.var.name.line);
    SimiValue nextMethod = block.closure.tryGet("#next" + block.closure.depth);
    if (nextMethod == null) {
      SimiObjectImpl iterable = (SimiObjectImpl) SimiObjectImpl.getOrConvertObject((SimiValue) evaluate(stmt.iterable), this);
      nextMethod = iterable.get(nextToken, 0, environment);
      if (nextMethod == null) {
        Token iterateToken = new Token(TokenType.IDENTIFIER, Constants.ITERATE, null, stmt.var.name.line);
        SimiObjectImpl iterator = (SimiObjectImpl) call(iterable.get(iterateToken, 0, environment), emptyArgs, iterateToken).getObject();
        nextMethod = iterator.get(nextToken, 0, environment);
      }
    }

    block.closure.assign(Token.named("#next" + block.closure.depth), nextMethod, true);
    loopBlocks.push(block);
    while (true) {
      SimiValue var = call(nextMethod, emptyArgs, nextToken);
      if (var == null) {
        this.environment.endBlock(stmt, yieldedStmts);
        break;
      }
      block.closure.assign(stmt.var.name, var, true);
      try {
        block.call(this, null, true);
      } catch (Return | Yield returnYield) {
        if (returnYield instanceof Return) {
          this.environment.endBlock(stmt, yieldedStmts);
        } else {
          putBlock(stmt, block);
        }
        loopBlocks.pop();
        throw returnYield;
      } catch (Break b) {
        break;
      } catch (Continue ignored) { }
    }
    loopBlocks.pop();
    return null;
  }

  @Override
  public Object visitAssignExpr(Expr.Assign expr) {
    Object value;
    if (expr.value instanceof Expr.Block) {
      value = visitFunctionStmt(new Stmt.Function(expr.name, (Expr.Block) expr.value));
    } else {
      value = evaluate(expr.value);
    }
    if (value instanceof TempNull) {
      value = null;
    }
//    else if (value instanceof SimiValue.String || value instanceof SimiValue.Number) {
//      value = value.copy();
//    }
    if (expr.name.lexeme.startsWith(Constants.MUTABLE)) {
      Integer distance = locals.get(expr);
      if (distance != null) {
        environment.assignAt(distance, expr.name, value);
      } else {
        globals.assign(expr.name, value, false);
      }
    } else {
      environment.assignAt(0, expr.name, value);
    }
    return value;
  }

  @Override
  public Object visitBinaryExpr(Expr.Binary expr) {
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right); // [left]

    switch (expr.operator.type) {
      case BANG_EQUAL: return !isEqual(left, right, expr);
      case EQUAL_EQUAL: return isEqual(left, right, expr);
      case LESS_GREATER: return compare(left, right, expr);
      case IS: return isInstance(left, right);
      case ISNOT: return !isInstance(left, right);
      case IN: return isIn(left, right, expr);
      case NOTIN: return !isIn(left, right, expr);
      case GREATER:
        checkNumberOperands(expr.operator, left, right);
        return getNumber(left) > getNumber(right);
      case GREATER_EQUAL:
        checkNumberOperands(expr.operator, left, right);
          return getNumber(left) >= getNumber(right);
      case LESS:
        checkNumberOperands(expr.operator, left, right);
          return getNumber(left) < getNumber(right);
      case LESS_EQUAL:
        checkNumberOperands(expr.operator, left, right);
          return getNumber(left) <= getNumber(right);
      case MINUS:
        checkNumberOperands(expr.operator, left, right);
          return getNumber(left) - getNumber(right);
      case PLUS: {
        if (left instanceof SimiValue.Number && right instanceof SimiValue.Number
                || left instanceof Double && right instanceof Double) {
          return getNumber(left) + getNumber(right);
        } // [plus]
        String leftStr = (left != null) ? getString(left) : "nil";
        String rightStr = (right != null) ? getString(right) : "nil";
        return leftStr + rightStr;
      }
      case SLASH:
        checkNumberOperands(expr.operator, left, right);
          return getNumber(left) / getNumber(right);
      case STAR:
        checkNumberOperands(expr.operator, left, right);
          return getNumber(left) * getNumber(right);
        case MOD:
            checkNumberOperands(expr.operator, left, right);
            return getNumber(left) % getNumber(right);
      case QUESTION_QUESTION:
        return (left != null) ? left : right;
    }

    // Unreachable.
    return null;
  }

  @Override
  public Object visitCallExpr(Expr.Call expr) {
    Object callee = evaluate(expr.callee);
    return call(callee, expr.arguments, expr.paren);
  }

  private Object call(Object callee, List<Expr> args, Token paren) {
    List<Object> arguments = new ArrayList<>();
    for (Expr arg : args) { // [in-order]
      Object value;
      if (arg instanceof Expr.Block) {
        value = new SimiValue.Callable(new BlockImpl((Expr.Block) arg, environment), null, null);
      } else {
        value = evaluate(arg);
      }
      arguments.add(value);
    }
    return call(callee, paren, arguments);
  }

  private Object call(Object callee, Token paren, List<Object> arguments) {
    SimiCallable callable;
    String methodName;
    SimiObject instance;
    if (callee instanceof SimiValue.Object || callee instanceof SimiObject) {
      SimiObject value = getObject(callee);
      if (!(value instanceof SimiClassImpl)) {
        throw new RuntimeError(paren,"Can only call functions and classes.");
      }
      return ((SimiClassImpl) value).init(this, arguments);
    } else if (callee instanceof SimiValue.Callable || callee instanceof SimiCallable) {
      callable = getCallable(callee);
      methodName = ((SimiValue.Callable) callee).name;
      instance = ((SimiValue.Callable) callee).getInstance();
    } else {
      throw new RuntimeError(paren,"Can only call functions and classes.");
    }

    if (arguments.size() != callable.arity()) {
      throw new RuntimeError(paren, "Expected " +
              callable.arity() + " arguments but got " +
              arguments.size() + ".");
    }
    boolean isNative = callable instanceof SimiFunction && ((SimiFunction) callable).isNative
            || callable instanceof SimiMethod && ((SimiMethod) callable).function.isNative
            || callable instanceof BlockImpl && ((BlockImpl) callable).isNative();
    if (isNative) {
      if (instance != null) {
        SimiClassImpl clazz;
        if (callable instanceof SimiMethod) {
          clazz = ((SimiMethod) callable).clazz;
        } else {
          if (instance instanceof SimiClassImpl) {
            clazz = (SimiClassImpl) instance;
          } else {
            clazz = (SimiClassImpl) instance.getSimiClass();
          }
        }
        boolean isBaseClass = isBaseClass(clazz.name);
        if (!isBaseClass) {
          try {
            return nativeModulesManager.call(clazz.name, methodName, instance, this, arguments);
          } catch (IllegalArgumentException ignored) { }
        }
        String className = isBaseClass ? clazz.name : Constants.CLASS_OBJECT;
        SimiCallable nativeMethod = baseClassesNativeImpl.get(className, methodName, callable.arity());
        if (nativeMethod == null) {
          nativeMethod = baseClassesNativeImpl.get(Constants.CLASS_GLOBALS, methodName, callable.arity());
        }
        List<SimiValue> nativeArgs = new ArrayList<>();
        nativeArgs.add(new SimiValue.Object(instance));
        nativeArgs.addAll(arguments);
        return nativeMethod.call(this, nativeArgs, false);
      } else {
        try {
          return nativeModulesManager.call(net.globulus.simi.api.Constants.GLOBALS_CLASS_NAME,
                  methodName, null, this, arguments);
        } catch (IllegalArgumentException ignored) { }
      }
    }
    return callable.call(this, arguments, false);
  }

  @Override
  public Object visitGetExpr(Expr.Get expr) {
    Object object = evaluate(expr.object);
    Token name = evaluateGetSetName(expr.origin, expr.name);
    if (object instanceof TempNull) {
      return TempNull.INSTANCE;
    }
    try {
        SimiObject simiObject = SimiObjectImpl.getOrConvertObject(object, this);
        if (simiObject == null) {
          return null;
        }
        if (simiObject instanceof SimiObjectImpl) {
          return ((SimiObjectImpl) simiObject).get(name, expr.arity, environment);
        } else {
          return simiObject.get(name.lexeme, environment);
        }
    } catch (SimiValue.IncompatibleValuesException e) {
        throw new RuntimeError(expr.origin,"Only instances have properties.");
    }
  }

  private Token evaluateGetSetName(Token origin, Expr name) {
    if (name instanceof Expr.Variable) {
      return ((Expr.Variable) name).name;
    } else {
      Object val = evaluate(name);
      String lexeme;
      if (val instanceof SimiValue.Number || val instanceof SimiValue.String) {
        lexeme = val.toString();
      } else {
        throw new RuntimeError(origin,"Unable to parse getter/setter, invalid value: " + val);
      }
      return new Token(TokenType.IDENTIFIER, lexeme, null, origin.line);
    }
  }

  @Override
  public Object visitGroupingExpr(Expr.Grouping expr) {
    return evaluate(expr.expression);
  }

  @Override
  public Object visitGuExpr(Expr.Gu expr) {
    Scanner scanner = new Scanner(expr.string.value + "\n");
    Parser parser = new Parser(scanner.scanTokens(true));
    return evaluate(((Stmt.Expression) parser.parse().get(0)).expression);
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
    if (!(object instanceof SimiValue.Object || object instanceof SimiObject)) { // [order]
      throw new RuntimeError(expr.origin, "Only objects have fields.");
    }

    Token name = evaluateGetSetName(expr.origin, expr.name);
    Object value;
    if (expr.value instanceof Expr.Block) {
      value = new SimiValue.Callable(new BlockImpl((Expr.Block) expr.value, environment), name.lexeme, getObject(object));
    } else {
      value = evaluate(expr.value);
    }
    ((SimiObjectImpl) getObject(object)).set(name, value, environment);
    return value;
  }

  @Override
  public Object visitSuperExpr(Expr.Super expr) {
    int distance = locals.get(expr);
    SimiMethod method = null;
    List<SimiClassImpl> superclasses = ((SimiClassImpl.SuperClassesList) environment.getAt(distance, Constants.SUPER)).value;
    if (expr.superclass != null) {
      superclasses = superclasses.stream()
              .filter(superclass -> superclass.name.equals(expr.superclass.lexeme)).collect(Collectors.toList());
      if (superclasses.isEmpty()) {
        throw new RuntimeError(expr.superclass, "Invalid superclass specified!");
      }
    }

    // "self" is always one level nearer than "super"'s environment.
    SimiObjectImpl object = (SimiObjectImpl) environment.getAt(distance - 1, Constants.SELF);

    for (SimiClassImpl superclass : superclasses) {
      method = superclass.findMethod(object, expr.method.lexeme, expr.arity);
      if (method != null) {
        break;
      }
    }

    if (method == null) {
      throw new RuntimeError(expr.method,
          "Undefined property '" + expr.method.lexeme + "'.");
    }

    return new SimiValue.Callable(method, expr.method.lexeme, object);
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
        return -getNumber(right);
      case QUESTION:
        return (right == null) ? TempNull.INSTANCE : right;
    }
    // Unreachable.
    return null;
  }

  @Override
  public Object visitVariableExpr(Expr.Variable expr) {
    return lookUpVariable(expr.name, expr);
  }

  @Override
  public SimiValue.Object visitObjectLiteralExpr(Expr.ObjectLiteral expr) {
      boolean immutable = (expr.opener.type == TokenType.LEFT_BRACKET);
      SimiClassImpl objectClass = getObjectClass();
      SimiObjectImpl object;
      if (expr.props.isEmpty()) {
        object = SimiObjectImpl.empty(objectClass, immutable);
      } else {
        LinkedHashMap<String, SimiValue> mapFields = new LinkedHashMap<>();
        ArrayList<SimiValue> arrayFields = new ArrayList<>();
        int count = 0;
        for (Expr prop : expr.props) {
          String key;
          Expr valueExpr;
          if (expr.isDictionary) {
            Expr.Assign assign = (Expr.Assign) prop;
            key = assign.name.lexeme;
            if (key == null) {
              key = assign.name.literal.getString();
            }
            valueExpr = assign.value;
          } else {
            key = Constants.IMPLICIT + count;
            valueExpr = prop;
          }
          SimiValue value;
          if (valueExpr instanceof Expr.Block) {
            value = new SimiValue.Callable(new BlockImpl((Expr.Block) valueExpr, environment), key, null);
          } else {
            value = evaluate(valueExpr);
          }
          if (expr.isDictionary) {
            mapFields.put(key, value);
          } else {
            arrayFields.add(value);
          }
          count++;
        }
        if (expr.isDictionary) {
          object = SimiObjectImpl.fromMap(objectClass, immutable, mapFields);
        } else {
          object = SimiObjectImpl.fromArray(objectClass, immutable, arrayFields);
        }
        for (SimiValue value : object.values()) {
          if (value instanceof SimiValue.Callable) {
            ((SimiValue.Callable) value).bind(object);
          }
        }
      }
      return new SimiValue.Object(object);
  }

    private void executeRescueBlock(Stmt.Rescue rescue, SimiException e) {
      List<SimiValue> args = new ArrayList<>();
      if (e != null) {
        args.add(new SimiValue.Object(e));
      } else {
        args.add(null);
      }
      call(new SimiValue.Callable(new BlockImpl(rescue.block, this.environment), null, null), rescue.keyword, args);
    }

    private SimiValue lookUpVariable(Token name, Expr expr) {
//        Integer distance = locals.get(expr);
        SimiValue value = null;
//        if (distance != null) {
//          value = environment.getAt(distance, name.lexeme);
//        }
//        if (value == null) {
          value = environment.tryGet(name.lexeme);
          if (value != null) {
            return value;
          }
//        }
        return globals.get(name);
    }

  private void checkNumberOperand(Token operator, Object operand) {
    if (operand instanceof Double || operand instanceof SimiValue.Number) {
      return;
    }
    throw new RuntimeError(operator, "Operand must be a number.");
  }

  private void checkNumberOperands(Token operator, Object left, Object right) {
    if (left instanceof Double && right instanceof Double) {
      return;
    }
    if (left instanceof SimiValue.Number && right instanceof SimiValue.Number) {
      return;
    }
    throw new RuntimeError(operator, "Operands must be numbers.");
  }

  static boolean isTruthy(Object object) {
    if (object == null || object == TempNull.INSTANCE) {
        return false;
    }
    try {
        double value = (Double) object;
        return value != 0;
    } catch (ClassCastException e) {
        return true;
    }
  }

  private boolean isEqual(Object a, Object b, Expr.Binary expr) {
    // nil is only equal to nil.
    if (a == null && b == null) {
      return true;
    }
    if (a == null) {
      return false;
    }
    if (a instanceof SimiValue.Object) {
      a = ((SimiValue.Object) a).getObject();
    }
    if (a instanceof SimiObject) {
      Token equals = new Token(TokenType.IDENTIFIER, Constants.EQUALS, null, expr.operator.line);
      return call(((SimiObjectImpl) a).get(equals, 1, environment), equals, Collections.singletonList(b)).getNumber() != 0;
    }
    return a.equals(b);
  }

  private Double compare(Object a, Object b, Expr.Binary expr) {
    // nil is only equal to nil.
    if (a == null && b == null) {
      return 1.0;
    }
    if (a == null) {
      return 0.0;
    }
    if (a instanceof SimiValue.Object) {
      a = ((SimiValue.Object) a).getObject();
    }
    if (a instanceof SimiObject) {
      Token compareTo = new Token(TokenType.IDENTIFIER, Constants.COMPARE_TO, null, expr.operator.line);
      return call(((SimiObjectImpl) a).get(compareTo, 1, environment), compareTo, Arrays.asList(a, b));
    }
    if (a instanceof Double) {
      return (double) Double.compare((Double) a, (Double) b);
    }
    if (a instanceof String) {
      return (double) ((String) a).compareTo((String) b);
    }
    return (double) ((SimiValue) a).compareTo((SimiValue) b);
  }

  private boolean isInstance(Object a, Object b) {
    SimiObject left = SimiObjectImpl.getOrConvertObject(a, this);
    SimiObject right = SimiObjectImpl.getOrConvertObject(b, this);
    if (left == null || right == null) {
      return false;
    }
    SimiClassImpl clazz = (SimiClassImpl) right;
    if (left instanceof SimiObjectImpl) {
      return ((SimiObjectImpl) left).is(clazz);
    } else {
      return left.getSimiClass() == clazz;
    }
  }

  private boolean isIn(Object a, Object b, Expr.Binary expr) {
      SimiObjectImpl object = (SimiObjectImpl) SimiObjectImpl.getOrConvertObject(b, this);
      Token has = new Token(TokenType.IDENTIFIER, Constants.HAS, null, expr.operator.line);
      return call(object.get(has, 1, environment), has, Collections.singletonList(a)).getNumber() != 0;
  }

  private String stringify(SimiValue object) {
    if (object == null) {
      return "nil";
    }
    return object.toString();
  }

  private boolean isBaseClass(String className) {
    return className.equals(Constants.CLASS_OBJECT)
            || className.equals(Constants.CLASS_NUMBER)
            || className.equals(Constants.CLASS_STRING)
            || className.equals(Constants.CLASS_EXCEPTION);
  }

  private SimiClassImpl getObjectClass() {
    return (SimiClassImpl) globals.tryGet(Constants.CLASS_OBJECT).getObject();
  }

  private void putBlock(Stmt.BlockStmt stmt, BlockImpl block) {
    SparseArray<BlockImpl> blocks = yieldedStmts.get(stmt);
    if (blocks == null) {
      blocks = new SparseArray<>();
      yieldedStmts.put(stmt, blocks);
    }
    blocks.put(this.environment.depth, block);
  }

  private SimiObject getObject(Object value) {
    if (value instanceof SimiObject) {
      return (SimiObject) value;
    }
    if (value instanceof SimiValue.Object) {
      return ((SimiValue.Object) value).getObject();
    }
    return null;
  }

  private Double getNumber(Object value) {
    if (value instanceof Double) {
      return (Double) value;
    }
    if (value instanceof SimiValue.Number) {
      return ((SimiValue.Number) value).getNumber();
    }
    return null;
  }

  private String getString(Object value) {
    if (value instanceof String) {
      return (String) value;
    }
    if (value instanceof SimiValue.String) {
      return ((SimiValue.String) value).getString();
    }
    return null;
  }

  private SimiCallable getCallable(Object value) {
    if (value instanceof SimiCallable) {
      return (SimiCallable) value;
    }
    if (value instanceof SimiValue.Callable) {
      return ((SimiValue.Callable) value).getCallable();
    }
    return null;
  }
}
