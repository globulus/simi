package net.globulus.simi;

import net.globulus.simi.api.*;

import java.util.*;
import java.util.stream.Collectors;

class Interpreter implements BlockInterpreter, Expr.Visitor<SimiProperty>, Stmt.Visitor<SimiProperty> {

  final Collection<NativeModulesManager> nativeModulesManagers;
  private final Environment globals = new Environment();
  private Environment environment = globals;
  private final Map<Expr, Integer> locals = new HashMap<>();
  private final BaseClassesNativeImpl baseClassesNativeImpl = new BaseClassesNativeImpl();
  private final Stack<SimiBlock> loopBlocks = new Stack<>();
  private final Stack<SimiException> raisedExceptions = new Stack<>();
  private final Map<Stmt.BlockStmt, SparseArray<BlockImpl>> yieldedStmts = new HashMap<>();

  private Map<Object, List<SimiObject>> annotations = new HashMap<>();
  private List<SimiObject> annotationsBuffer = new ArrayList<>();

  static Interpreter sharedInstance;

  Interpreter(Collection<NativeModulesManager> nativeModulesManagers) {
    sharedInstance = this;
    this.nativeModulesManagers = nativeModulesManagers;
    globals.define("clock", new SimiValue.Callable(new SimiCallable() {

      @Override
      public String toCode(int indentationLevel, boolean ignoreFirst) {
        return "clock()";
      }

      @Override
      public int arity() {
        return 0;
      }

      @Override
      public SimiProperty call(BlockInterpreter interpreter,
                          List<SimiProperty> arguments,
                            boolean rethrow) {
        return new SimiValue.Number((double)System.currentTimeMillis() / 1000.0);
      }
    }, "clock", null));
  }

  SimiProperty interpret(List<Stmt> statements) {
    SimiProperty result = null;
    try {
      for (Stmt statement : statements) {
        if (raisedExceptions.isEmpty()) {
          result = execute(statement);
        } else {
          throw raisedExceptions.peek();
        }
      }
    } catch (RuntimeError error) {
      ErrorHub.sharedInstance().runtimeError(error);
    }
    return result;
  }

  private SimiProperty evaluate(Expr expr) {
    return expr.accept(this);
  }

  private SimiProperty evaluate(Expr expr, Object... params) {
    return expr.accept(this, params);
  }

  private SimiProperty execute(Stmt stmt) {
    return stmt.accept(this);
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
            } else if (block.canReturn()) {
              throw new Return(null);
            } else {
              throw new Break();
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
  public SimiProperty getGlobal(String name) {
    return globals.getAt(0, name);
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
  public SimiObject newObject(boolean immutable, LinkedHashMap<String, SimiProperty> props) {
    return SimiObjectImpl.fromMap(getObjectClass(), immutable, props);
  }

  @Override
  public SimiObject newArray(boolean immutable, ArrayList<SimiValue> props) {
    return SimiObjectImpl.fromArray(getObjectClass(), immutable, props);
  }

  @Override
  public SimiObject newInstance(SimiClass clazz, LinkedHashMap<String, SimiProperty> props) {
    return SimiObjectImpl.instance((SimiClassImpl) clazz, props);
  }

  @Override
  public SimiValue visitBlockExpr(Expr.Block stmt, boolean newScope, boolean execute) {
    if (execute) {
      executeBlock(stmt, new Environment(environment), 0);
      return null;
    }
    return new SimiValue.Callable(new BlockImpl(stmt, environment), null, null);
  }

  @Override
  public SimiProperty visitAnnotationStmt(Stmt.Annotation stmt) {
    SimiObject object = SimiObjectImpl.getOrConvertObject(evaluate(stmt.expr).getValue(), this);
    annotationsBuffer.add(object);
    return null;
  }

  @Override
  public SimiProperty visitBreakStmt(Stmt.Break stmt) {
    if (loopBlocks.isEmpty()) {
      ErrorHub.sharedInstance().error(stmt.name, "Break outside a loop!");
    }
    throw new Break();
  }

  @Override
  public SimiProperty visitClassStmt(Stmt.Class stmt, boolean addToEnv) {
      applyAnnotations(stmt);
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
            SimiObject clazz = evaluate(superclass).getValue().getObject();
            if (!(clazz instanceof SimiClassImpl)) {
                throw new RuntimeError(stmt.name, "Superclass must be a class.");
            }
            SimiClassImpl simiClass = (SimiClassImpl) clazz;
            if (simiClass.type == SimiClassImpl.Type.FINAL) {
              throw new RuntimeError(stmt.name, "Can't use a final class as superclass: " + simiClass.name);
            }
            superclasses.add(simiClass);
        }
      } else if (!isBaseClass) {
          superclasses = Collections.singletonList(getObjectClass());
      }
      environment = new Environment(environment);
      environment.define(Constants.SUPER, new SimiClassImpl.SuperClassesList(superclasses));

      Map<String, SimiProperty> constants = new HashMap<>();
      for (Expr.Assign constant : stmt.constants) {
        if (constant.annotations != null) {
          for (Stmt.Annotation annotation : constant.annotations) {
            visitAnnotationStmt(annotation);
          }
          applyAnnotations(constant);
        }
          String key = constant.name.lexeme;
          SimiProperty prop = evaluate(constant.value);
          List<SimiObject> annotations = getAnnotations(constant);
          if (prop != null) {
            constants.put(key, new SimiPropertyImpl(prop.getValue(), annotations));
          }
      }
      for (Stmt.Class innerClass : stmt.innerClasses) {
        if (innerClass.annotations != null) {
          for (Stmt.Annotation annotation : innerClass.annotations) {
            visitAnnotationStmt(annotation);
          }
          applyAnnotations(innerClass);
        }
        String key = innerClass.name.lexeme;
        SimiProperty prop = visitClassStmt(innerClass, false);
        List<SimiObject> annotations = getAnnotations(innerClass);
        if (prop != null) {
          constants.put(key, new SimiPropertyImpl(prop.getValue(), annotations));
        }
      }

    Map<OverloadableFunction, SimiFunction> methods = new HashMap<>();
    for (Stmt.Function method : stmt.methods) {
        if (method.annotations != null) {
            for (Stmt.Annotation annotation : method.annotations) {
                visitAnnotationStmt(annotation);
            }
            applyAnnotations(method);
        }
        String name = method.name.lexeme;
      SimiFunction function = new SimiFunction(method, environment,
          name.equals(Constants.INIT), method.block.isNative, getAnnotations(method));
      methods.put(new OverloadableFunction(name, function.arity()), function);
    }

    SimiClassImpl klass = new SimiClassImpl(SimiClassImpl.Type.from(stmt.opener.type),
            className, superclasses, constants, methods, stmt);
    SimiValue classValue = new SimiValue.Object(klass);
    SimiProperty classProp = new SimiPropertyImpl(classValue, getAnnotations(stmt));

//    if (superclass != null) {
//      environment = environment.enclosing;
//    }

    if (isBaseClass) {
        globals.assign(stmt.name, classProp, false);
    } else if (addToEnv) {
        environment.assign(stmt.name, classProp, false);
    }
    return classProp;
  }

  @Override
  public SimiProperty visitContinueStmt(Stmt.Continue stmt) {
    if (loopBlocks.isEmpty()) {
      ErrorHub.sharedInstance().error(stmt.name, "Continue outside a loop!");
    }
    throw new Continue();
  }

  @Override
  public SimiProperty visitExpressionStmt(Stmt.Expression stmt) {
    return evaluate(stmt.expression);
  }

  @Override
  public SimiValue visitFunctionStmt(Stmt.Function stmt) {
      applyAnnotations(stmt);
    SimiFunction function = new SimiFunction(stmt, environment, false, stmt.block.isNative, getAnnotations(stmt));
    SimiValue value = new SimiValue.Callable(function, stmt.name.lexeme, null);
    environment.define(stmt.name.lexeme, new SimiPropertyImpl(value, function.annotations));
    return value;
  }

  @Override
  public SimiProperty visitElsifStmt(Stmt.Elsif stmt) {
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
      return new SimiValue.Number(true);
    }
    return new SimiValue.Number(false);
  }

  @Override
  public SimiProperty visitIfStmt(Stmt.If stmt) {
    if (visitElsifStmt(stmt.ifstmt).getValue().getNumber() != 0) {
      return null;
    }
    for (Stmt.Elsif elsif : stmt.elsifs) {
      if (visitElsifStmt(elsif).getValue().getNumber() != 0) {
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
  public SimiProperty visitPrintStmt(Stmt.Print stmt) {
    SimiProperty prop = evaluate(stmt.expression);
    System.out.println(stringify(prop));
    return prop;
  }

  @Override
  public SimiProperty visitRescueStmt(Stmt.Rescue stmt) {
    executeRescueBlock(stmt, null);
    return null;
  }

  @Override
  public SimiProperty visitReturnStmt(Stmt.Return stmt) {
    SimiProperty prop = null;
    if (stmt.value != null) {
      prop = evaluate(stmt.value);
    }
    throw new Return(prop);
  }

  @Override
  public SimiProperty visitYieldStmt(Stmt.Yield stmt) {
    SimiProperty prop = null;
    if (stmt.value != null) {
      prop = evaluate(stmt.value);
    }
    throw new Yield(prop, false);
  }

  @Override
  public SimiProperty visitWhileStmt(Stmt.While stmt) {
    loopBlocks.push(stmt.body);
    BlockImpl block = this.environment.getOrAssignBlock(stmt, stmt.body, yieldedStmts);
    while (isTruthy(evaluate(stmt.condition).getValue())) {
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
  public SimiProperty visitForStmt(Stmt.For stmt) {
    BlockImpl block = this.environment.getOrAssignBlock(stmt, stmt.body, yieldedStmts);

    List<Expr> emptyArgs = new ArrayList<>();
    Token nextToken = new Token(TokenType.IDENTIFIER, Constants.NEXT, null, stmt.var.name.line);
    String nextMethodName = "#next" + block.closure.depth;
    SimiProperty nextMethod = block.closure.tryGet(nextMethodName);
    if (nextMethod == null) {
      SimiObjectImpl iterable = (SimiObjectImpl) SimiObjectImpl.getOrConvertObject(evaluate(stmt.iterable), this);
      if (iterable == null) {
        return null;
      }
      nextMethod = iterable.get(nextToken, 0, environment);
      if (nextMethod == null) {
        Token iterateToken = new Token(TokenType.IDENTIFIER, Constants.ITERATE, null, stmt.var.name.line);
        SimiObjectImpl iterator = (SimiObjectImpl) call(iterable.get(iterateToken, 0, environment), emptyArgs, iterateToken)
                .getValue().getObject();
        nextMethod = iterator.get(nextToken, 0, environment);
      }
    }

    block.closure.assign(Token.named("#next" + block.closure.depth), nextMethod, true);
    loopBlocks.push(block);
    while (true) {
      SimiProperty var = call(nextMethod.getValue(), emptyArgs, nextToken);
      if (var == null) {
        this.environment.endBlock(stmt, yieldedStmts);
        break;
      }
      block.closure.assign(stmt.var.name, var,true);
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
    block.closure.define(nextMethodName, null);
    return null;
  }

    @Override
    public SimiValue visitAnnotationsExpr(Expr.Annotations expr) {
        SimiProperty object = environment.tryGet(expr.tokens.get(0).lexeme);
          for (int i = 1; i < expr.tokens.size(); i++) {
            object = object.getValue().getObject().get(expr.tokens.get(i).lexeme, environment);
        }
        if (object == null || object.getAnnotations() == null) {
            return null;
        }
        ArrayList<SimiValue> annotations = object.getAnnotations().stream()
                .map(SimiValue.Object::new)
                .collect(Collectors.toCollection(ArrayList::new));
        return new SimiValue.Object(SimiObjectImpl.fromArray(getObjectClass(), true, annotations));
    }

    @Override
  public SimiProperty visitAssignExpr(Expr.Assign expr) {
    if (expr.value instanceof Expr.Block) {
      return visitFunctionStmt(new Stmt.Function(expr.name, (Expr.Block) expr.value, null));
    }
    applyAnnotations(expr);
    SimiProperty prop = evaluate(expr.value);
    SimiValue value;
    if (prop == null || prop instanceof TempNull || prop.getValue() instanceof TempNull) {
      value = null;
    } else if (prop instanceof SimiValue.String || prop instanceof SimiValue.Number) {
      value = prop.getValue().copy();
    } else {
      value = prop.getValue();
    }
    List<SimiObject> assignAnnotations = getAnnotations(expr);
    if (assignAnnotations == null && prop != null && prop.getAnnotations() != null) {
      assignAnnotations = prop.getAnnotations();
    }
    SimiProperty newProp = new SimiPropertyImpl(value, assignAnnotations);
    if (expr.name.lexeme.startsWith(Constants.MUTABLE)) {
      Integer distance = locals.get(expr);
      if (distance != null) {
        environment.assignAt(distance, expr.name, newProp);
      } else {
        globals.assign(expr.name, newProp, false);
      }
    } else {
      environment.assignAt(0, expr.name, newProp);
    }
    return newProp;
  }

  @Override
  public SimiValue visitBinaryExpr(Expr.Binary expr) {
    SimiProperty leftProp = evaluate(expr.left);
    SimiProperty rightProp = evaluate(expr.right);
    SimiValue left = (leftProp != null) ? leftProp.getValue() : null;
    SimiValue right = (rightProp != null) ? rightProp.getValue() : null;

    switch (expr.operator.type) {
      case BANG_EQUAL: return new SimiValue.Number(!isEqual(left, right, expr));
      case EQUAL_EQUAL: return new SimiValue.Number(isEqual(left, right, expr));
      case LESS_GREATER: return compare(left, right, expr);
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
      case PLUS: {
        if (left instanceof SimiValue.Number && right instanceof SimiValue.Number) {
          return new SimiValue.Number(left.getNumber() + right.getNumber());
        } // [plus]
        String leftStr = (left != null) ? left.toString() : "nil";
        String rightStr = (right != null) ? right.toString() : "nil";
        return new SimiValue.String(leftStr + rightStr);
      }
      case SLASH:
        checkNumberOperands(expr.operator, left, right);
          return new SimiValue.Number(left.getNumber() / right.getNumber());
      case SLASH_SLASH:
        checkNumberOperands(expr.operator, left, right);
        return new SimiValue.Number(left.getNumber().longValue() / right.getNumber().longValue());
      case STAR:
        checkNumberOperands(expr.operator, left, right);
          return new SimiValue.Number(left.getNumber() * right.getNumber());
        case MOD:
            checkNumberOperands(expr.operator, left, right);
            return new SimiValue.Number(left.getNumber() % right.getNumber());
      case QUESTION_QUESTION:
        return (left != null) ? left : right;
    }

    // Unreachable.
    return null;
  }

  @Override
  public SimiProperty visitCallExpr(Expr.Call expr) {
    SimiProperty callee = evaluate(expr.callee);
    SimiProperty result = call(callee, expr.arguments, expr.paren);
    return result;
  }

  private SimiProperty call(SimiProperty prop, List<Expr> args, Token paren) {
    SimiValue callee = (prop != null) ? prop.getValue() :  null;
    return call(callee, args, paren);
  }

  private SimiProperty call(SimiValue callee, List<Expr> args, Token paren) {
    List<SimiProperty> arguments = new ArrayList<>();
    for (Expr arg : args) { // [in-order]
      SimiProperty prop;
      if (arg instanceof Expr.Block) {
        prop = new SimiValue.Callable(new BlockImpl((Expr.Block) arg, environment), null, null);
      } else {
        prop = evaluate(arg);
      }
      arguments.add(prop);
    }
    return call(callee, paren, arguments);
  }

  private SimiProperty call(SimiValue callee, Token paren, List<SimiProperty> arguments) {
    SimiCallable callable;
    String methodName;
    SimiObject instance;
    if (callee instanceof SimiValue.Object) {
      SimiObject value = callee.getObject();
      if (value instanceof SimiClassImpl) {
        return ((SimiClassImpl) value).init(this, arguments);
      } else {
        return callee;
      }
    } else if (callee instanceof SimiValue.Callable) {
      callable = callee.getCallable();
      methodName = ((SimiValue.Callable) callee).name;
      instance = ((SimiValue.Callable) callee).getInstance();
    } else if (callee instanceof TempNull) {
      return TempNull.INSTANCE;
    } else {
      return callee;
    }

    List<SimiProperty> decomposedArgs = arguments;
    if (arguments.size() != callable.arity()) {
      // See if we can decompose argument array/object into actual arguments
      decomposedArgs = decomposeArguments(callable, arguments);
      if (decomposedArgs == arguments) { // We didn't manage to decomp args from object
        throw new RuntimeError(paren, "Expected " +
                callable.arity() + " arguments but got " +
                arguments.size() + ".");
      }
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
          for (NativeModulesManager manager : nativeModulesManagers) {
            try {
              return manager.call(clazz.name, methodName, instance, this, decomposedArgs);
            } catch (IllegalArgumentException ignored) {
            }
          }
        }
        String className = isBaseClass ? clazz.name : Constants.CLASS_OBJECT;
        SimiCallable nativeMethod = baseClassesNativeImpl.get(className, methodName, callable.arity());
        if (nativeMethod == null) {
          nativeMethod = baseClassesNativeImpl.get(Constants.CLASS_GLOBALS, methodName, callable.arity());
        }
        List<SimiProperty> nativeArgs = new ArrayList<>();
        nativeArgs.add(new SimiValue.Object(instance));
        nativeArgs.addAll(decomposedArgs);
        return nativeMethod.call(this, nativeArgs, false);
      } else {
        for (NativeModulesManager manager : nativeModulesManagers) {
          try {
            return manager.call(net.globulus.simi.api.Constants.GLOBALS_CLASS_NAME,
                    methodName, null, this, decomposedArgs);
          } catch (IllegalArgumentException ignored) {
          }
        }
      }
    }
    return callable.call(this, decomposedArgs, false);
  }

  private List<SimiProperty> decomposeArguments(SimiCallable callable, List<SimiProperty> arguments) {
    if (arguments.size() == 1 && arguments.get(0).getValue() instanceof SimiValue.Object) {
      SimiObject argObject = arguments.get(0).getValue().getObject();
      List<SimiValue> values = argObject.values();
      if (argObject.values().size() == callable.arity()) {
        return values.stream().map(v -> (SimiProperty) v).collect(Collectors.toList());
      }
    }
    return arguments;
  }

  @Override
  public SimiProperty visitGetExpr(Expr.Get expr) {
    SimiProperty object = evaluate(expr.object);
    Token name = evaluateGetSetName(expr.origin, expr.name);
    if (object instanceof TempNull) {
      return TempNull.INSTANCE;
    }
    try {
        SimiObject simiObject = SimiObjectImpl.getOrConvertObject(object, this);
        if (simiObject == null) {
          return null;
        }
        SimiProperty prop;
        if (simiObject instanceof SimiObjectImpl) {
          prop = ((SimiObjectImpl) simiObject).get(name, expr.arity, environment);
        } else {
          prop = simiObject.get(name.lexeme, environment);
        }
        return prop;
    } catch (SimiValue.IncompatibleValuesException e) {
        throw new RuntimeError(expr.origin,"Only instances have properties.");
    }
  }

  private Token evaluateGetSetName(Token origin, Expr name) {
    if (name instanceof Expr.Variable) {
      return ((Expr.Variable) name).name;
    } else {
      SimiValue val = evaluate(name).getValue();
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
  public SimiProperty visitGroupingExpr(Expr.Grouping expr) {
    if (expr.expression instanceof Expr.Block) {
      return evaluate(expr.expression, true, false);
    }
    return evaluate(expr.expression);
  }

  @Override
  public SimiProperty visitGuExpr(Expr.Gu expr) {
    String string = evaluate(expr.expr).getValue().getString();
    Scanner scanner = new Scanner(string + "\n");
    Parser parser = new Parser(scanner.scanTokens(true));
    Stmt stmt = parser.parse().get(0);
    if (stmt instanceof Stmt.Class) {
      return visitClassStmt((Stmt.Class) stmt, false);
    } else if (stmt instanceof Stmt.Expression) {
      return evaluate(((Stmt.Expression) stmt).expression);
    } else {
      ErrorHub.sharedInstance().error(0, "Invalid GU expression!");
      return null;
    }
  }

  @Override
  public SimiProperty visitIvicExpr(Expr.Ivic expr) {
    SimiValue value = evaluate(expr.expr).getValue();
    String code = value.toCode(0, false)
            .replace("end\n,", "end,")
            .replace("end\n)", "end)");
    return new SimiValue.String(code);
  }

  @Override
  public SimiValue visitLiteralExpr(Expr.Literal expr) {
    return expr.value;
  }

  @Override
  public SimiProperty visitLogicalExpr(Expr.Logical expr) {
    SimiProperty leftProp = evaluate(expr.left);
    SimiValue left = (leftProp != null) ? leftProp.getValue() : null;

    if (expr.operator.type == TokenType.OR) {
      if (isTruthy(left)) return left;
    } else {
      if (!isTruthy(left)) return left;
    }

    return evaluate(expr.right);
  }

  @Override
  public SimiProperty visitSetExpr(Expr.Set expr) {
    SimiProperty object = evaluate(expr.object);

    if (!(object != null && object.getValue() instanceof SimiValue.Object) && !(object instanceof SimiValue.Object)) { // [order]
      throw new RuntimeError(expr.origin, "Only objects have fields.");
    }

    Token name = evaluateGetSetName(expr.origin, expr.name);
    SimiProperty prop;
    if (expr.value instanceof Expr.Block) {
      prop = new SimiValue.Callable(new BlockImpl((Expr.Block) expr.value, environment), name.lexeme, object.getValue().getObject());
    } else {
      prop = evaluate(expr.value);
    }
    ((SimiObjectImpl) object.getValue().getObject()).set(name, prop, environment);
    return prop;
  }

  @Override
  public SimiValue visitSuperExpr(Expr.Super expr) {
    int distance = locals.get(expr);
    SimiMethod method = null;
    List<SimiClassImpl> superclasses = ((SimiClassImpl.SuperClassesList) environment.getAt(distance, Constants.SUPER).getValue()).value;
    if (expr.superclass != null) {
      superclasses = superclasses.stream()
              .filter(superclass -> superclass.name.equals(expr.superclass.lexeme)).collect(Collectors.toList());
      if (superclasses.isEmpty()) {
        throw new RuntimeError(expr.superclass, "Invalid superclass specified!");
      }
    }

    // "self" is always one level nearer than "super"'s environment.
    SimiObjectImpl object = (SimiObjectImpl) environment.getAt(distance - 1, Constants.SELF).getValue().getObject();

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
  public SimiProperty visitSelfExpr(Expr.Self expr) {
    return lookUpVariable(expr.keyword, expr);
  }

  @Override
  public SimiValue visitUnaryExpr(Expr.Unary expr) {
    SimiProperty rightProp = evaluate(expr.right);
    SimiValue right = (rightProp != null) ? rightProp.getValue() : null;

    switch (expr.operator.type) {
        case NOT:
        return new SimiValue.Number(!isTruthy(right));
      case MINUS:
        checkNumberOperand(expr.operator, right);
        return new SimiValue.Number(-right.getNumber());
      case QUESTION:
        return (right == null) ? TempNull.INSTANCE : right;
    }
    // Unreachable.
    return null;
  }

  @Override
  public SimiProperty visitVariableExpr(Expr.Variable expr) {
    return lookUpVariable(expr.name, expr);
  }

    @Override
    public SimiValue visitObjectLiteralExpr(Expr.ObjectLiteral expr) {
        boolean immutable = (expr.opener.type == TokenType.LEFT_BRACKET);
        SimiClassImpl objectClass = getObjectClass();
        SimiObjectImpl object;
        if (expr.props.isEmpty()) {
          object = SimiObjectImpl.empty(objectClass, immutable);
        } else {
          LinkedHashMap<String, SimiProperty> mapFields = new LinkedHashMap<>();
          ArrayList<SimiProperty> arrayFields = new ArrayList<>();
          int count = 0;
          for (Expr propExpr : expr.props) {
            String key;
            Expr valueExpr;
            if (expr.isDictionary) {
              Expr.Assign assign = (Expr.Assign) propExpr;
              key = assign.name.lexeme;
              if (key == null) {
                key = assign.name.literal.getString();
              }
              valueExpr = assign.value;
            } else {
              key = Constants.IMPLICIT + count;
              valueExpr = propExpr;
            }
            SimiProperty prop;
            if (valueExpr instanceof Expr.Block) {
              prop = new SimiValue.Callable(new BlockImpl((Expr.Block) valueExpr, environment), key, null);
            } else {
              prop = evaluate(valueExpr);
            }
            if (key.equals(TokenType.CLASS.toCode())
                    && prop.getValue() instanceof SimiValue.Object
                    && prop.getValue().getObject() instanceof SimiClassImpl) {
              objectClass = (SimiClassImpl) prop.getValue().getObject();
            } else {
              if (expr.isDictionary) {
                mapFields.put(key, prop);
              } else {
                arrayFields.add(prop);
              }
              count++;
            }
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
      List<SimiProperty> args = new ArrayList<>();
      if (e != null) {
        args.add(new SimiValue.Object(e));
      } else {
        args.add(null);
      }
      call(new SimiValue.Callable(new BlockImpl(rescue.block, this.environment), null, null), rescue.keyword, args);
    }

    private SimiProperty lookUpVariable(Token name, Expr expr) {
//        Integer distance = locals.get(expr);
        SimiProperty prop = null;
//        if (distance != null) {
//          value = environment.getAt(distance, name.lexeme);
//        }
//        if (value == null) {
          prop = environment.tryGet(name.lexeme);
          if (prop != null && prop.getValue() != null) {
            return prop;
          }
//        }
        return globals.get(name);
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

  static boolean isTruthy(SimiProperty object) {
    if (object == null || object == TempNull.INSTANCE
            || object.getValue() == null || object.getValue() == TempNull.INSTANCE) {
        return false;
    }
    try {
        double value = object.getValue().getNumber();
        return value != 0;
    } catch (SimiValue.IncompatibleValuesException e) {
        return true;
    }
  }

  private boolean isEqual(SimiValue a, SimiValue b, Expr.Binary expr) {
    // nil is only equal to nil.
    if (a == null && b == null) {
      return true;
    }
    if (a == null) {
      return false;
    }
    if (a instanceof SimiValue.Object) {
      Token equals = new Token(TokenType.IDENTIFIER, Constants.EQUALS, null, expr.operator.line);
      return call(((SimiObjectImpl) a.getObject()).get(equals, 1, environment).getValue(),
              equals, Collections.singletonList(b)).getValue().getNumber() != 0;
    }
    return a.equals(b);
  }

  private SimiValue compare(SimiValue a, SimiValue b, Expr.Binary expr) {
    // nil is only equal to nil.
    if (a == null && b == null) {
      return SimiValue.Number.TRUE;
    }
    if (a == null) {
      return SimiValue.Number.FALSE;
    }
    if (a instanceof SimiValue.Object) {
      Token compareTo = new Token(TokenType.IDENTIFIER, Constants.COMPARE_TO, null, expr.operator.line);
      return call(((SimiObjectImpl) a.getObject()).get(compareTo, 1, environment).getValue(), compareTo, Arrays.asList(a, b)).getValue();
    }
    return new SimiValue.Number(a.compareTo(b));
  }

  private boolean isInstance(SimiValue a, SimiValue b, Expr.Binary expr) {
    SimiObject right = SimiObjectImpl.getOrConvertObject(b, this);
    if (a instanceof SimiValue.Callable
            && right instanceof SimiClassImpl
            && ((SimiClassImpl) right).name.equals(Constants.CLASS_FUNCTION)) {
      return true;
    }
    SimiObject left = SimiObjectImpl.getOrConvertObject(a, this);
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

  private boolean isIn(SimiValue a, SimiValue b, Expr.Binary expr) {
      SimiObjectImpl object;
      if (b instanceof SimiValue.Object) {
        object = ((SimiObjectImpl) b.getObject());
      } else {
        object = (SimiObjectImpl) SimiObjectImpl.getOrConvertObject(b, this);
//          throw new RuntimeError(expr.operator, "Right side must be an Object!");
      }
      Token has = new Token(TokenType.IDENTIFIER, Constants.HAS, null, expr.operator.line);
      return call(object.get(has, 1, environment).getValue(), has, Collections.singletonList(a)).getValue().getNumber() != 0;
  }

  private String stringify(SimiProperty object) {
    if (object == null) {
      return "nil";
    }
    return object.toString();
  }

  private boolean isBaseClass(String className) {
    return className.equals(Constants.CLASS_OBJECT)
            || className.equals(Constants.CLASS_NUMBER)
            || className.equals(Constants.CLASS_STRING)
            || className.equals(Constants.CLASS_FUNCTION)
            || className.equals(Constants.CLASS_EXCEPTION);
  }

  private SimiClassImpl getObjectClass() {
    return (SimiClassImpl) globals.tryGet(Constants.CLASS_OBJECT).getValue().getObject();
  }

  private void putBlock(Stmt.BlockStmt stmt, BlockImpl block) {
    SparseArray<BlockImpl> blocks = yieldedStmts.get(stmt);
    if (blocks == null) {
      blocks = new SparseArray<>();
      yieldedStmts.put(stmt, blocks);
    }
    blocks.put(this.environment.depth, block);
  }

  private void applyAnnotations(Object key) {
    if (annotationsBuffer.isEmpty()) {
      return;
    }
    List<SimiObject> copy = Collections.unmodifiableList(new ArrayList<>(annotationsBuffer));
    annotations.put(key, copy);
    annotationsBuffer.clear();
  }

  private List<SimiObject> getAnnotations(Object key) {
      List<SimiObject> list = annotations.get(key);
      annotations.remove(key);
      return list;
  }

  public List<String> defineTempVars(SimiProperty... vars) {
    long timestamp = System.currentTimeMillis();
    String prefix = "_tempvar_";
    List<String> names = new ArrayList<>(vars.length);
    int count = 0;
    for (SimiProperty var : vars) {
      String name = prefix + (timestamp + count);
      count++;
      names.add(name);
      environment.define(name, var);
    }
    return names;
  }

  public void undefineTempVars(List<String> names) {
    for (String name : names) {
      environment.define(name, null);
    }
  }
}
