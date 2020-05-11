package net.globulus.simi;

import net.globulus.simi.api.*;

import java.util.*;
import java.util.stream.Collectors;

class Interpreter implements
        BlockInterpreter,
        Expr.Visitor<SimiProperty>,
        Stmt.Visitor<SimiProperty>,
        Debugger.Evaluator {

    private static final String FILE_RUNTIME = "Runtime";

  final Collection<NativeModulesManager> nativeModulesManagers;
  private List<Stmt> statements;
  private final Environment globals = new Environment();
  private Environment environment = globals;
  private final Map<Expr, Integer> locals = new HashMap<>();
  private final BaseClassesNativeImpl baseClassesNativeImpl = new BaseClassesNativeImpl();
  private final Stack<SimiBlock> loopBlocks = new Stack<>();
  private final Stack<SimiExceptionWithDebugInfo> raisedExceptions = new Stack<>();
  private final Map<Stmt.BlockStmt, SparseArray<BlockImpl>> yieldedStmts = new HashMap<>();

  private Map<Object, List<SimiObject>> annotations = new HashMap<>();
  private List<SimiObject> annotationsBuffer = new ArrayList<>();

  private boolean addClassesToRootEnv;

  private Debugger debugger;

  static Interpreter sharedInstance;

  Interpreter(Collection<NativeModulesManager> nativeModulesManagers, Debugger debugger) {
    sharedInstance = this;
    this.nativeModulesManagers = nativeModulesManagers;
    this.debugger = debugger;
    if (debugger != null) {
        debugger.setEvaluator(this);
    }
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
                          SimiEnvironment environment,
                          List<SimiProperty> arguments,
                            boolean rethrow) {
        return new SimiValue.Number(System.currentTimeMillis());
      }
    }, "clock", null));
    globals.define("guid", new SimiValue.Callable(new SimiCallable() {

      @Override
      public String toCode(int indentationLevel, boolean ignoreFirst) {
        return "guid()";
      }

      @Override
      public int arity() {
        return 0;
      }

      @Override
      public SimiProperty call(BlockInterpreter interpreter,
                               SimiEnvironment environment,
                               List<SimiProperty> arguments,
                               boolean rethrow) {
        return new SimiValue.String(UUID.randomUUID().toString());
      }
    }, "guid", null));
  }

  SimiProperty interpret(List<Stmt> statements) {
   return interpret(statements, false);
  }

  SimiProperty interpret(List<Stmt> statements, boolean addClassesToRootEnv) {
    this.addClassesToRootEnv = addClassesToRootEnv;
    this.statements = statements;
    SimiProperty result = null;
    try {
      for (Stmt statement : statements) {
        if (raisedExceptions.isEmpty()) {
          result = execute(statement);
        } else {
          SimiExceptionWithDebugInfo e = raisedExceptions.peek();
          if (debugger != null) {
            debugger.triggerException(statement, e, true);
          }
          throw e.exception;
        }
      }
    } catch (RuntimeError error) {
      ErrorHub.sharedInstance().runtimeError(error);
    } finally {
      this.addClassesToRootEnv = false;
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
      debug(stmt);
    return stmt.accept(this);
  }

  private void debug(Stmt stmt) {
    if (debugger == null) {
        return;
    }
//    int index = statements.indexOf(stmt);
    Codifiable[] before = null;
//    if (index > 1) {
//      before = new Codifiable[] { statements.get(index - 2), statements.get(index - 1) };
//    }
    Codifiable[] after = null;
//    if (index < statements.size() - 2) {
//      after = new Codifiable[] { statements.get(index + 1), statements.get(index + 2) };
//    }
    debugger.pushLine(new Debugger.Frame(environment.deepClone(), environment, stmt, before, after));
    debugger.triggerBreakpoint(stmt);
  }

  void resolve(Expr expr, int depth) {
    locals.put(expr, depth);
  }

  @Override
  public SimiProperty executeBlock(SimiBlock block, SimiEnvironment environment, int startAt) {
    Environment previous = this.environment;
    SimiProperty returnValue = null;
    try {
      this.environment = (Environment) environment;
      List<? extends SimiStatement> statements = block.getStatements();
      int size = statements.size();
      for (int i = startAt < size ? startAt : 0; i < size; i++) {
        try {
          if (raisedExceptions.isEmpty()) {
            Stmt statement = (Stmt) statements.get(i);
            returnValue = execute(statement);
          } else { // Look for nearest rescue block
            Stmt.Rescue rescue = null;
            for (; i < size; i++) {
              Stmt statement = (Stmt) statements.get(i);
              if (statement instanceof Stmt.Rescue) {
                rescue = (Stmt.Rescue) statement;
                break;
              }
            }
            if (rescue != null) { // If not rescue block is available in this scope, maybe one is in scopes above
              SimiExceptionWithDebugInfo e = raisedExceptions.pop();
              executeRescueBlock(rescue, e.exception);
            } else if (block.canReturn()) {
              throw new Return(null);
            } else {
              throw new Break(raisedExceptions.peek().exception);
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
    return returnValue;
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
    Debugger.Capture capture = (debugger != null) ? debugger.copyCapture() : null;
    SimiExceptionWithDebugInfo edi = new SimiExceptionWithDebugInfo(e, capture);
    raisedExceptions.push(edi);
    if (debugger != null) {
      debugger.triggerException(null, edi, false);
    }
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
    SimiProperty annot = evaluate(stmt.expr);
    if (annot == null) {
      return null;
    }
    SimiObject object = SimiObjectImpl.getOrConvertObject(annot.getValue(), this);
    annotationsBuffer.add(object);
    return null;
  }

  @Override
  public SimiProperty visitBreakStmt(Stmt.Break stmt) {
    if (loopBlocks.isEmpty()) {
      runtimeError(stmt.name, "Break outside a loop!");
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
                runtimeError(stmt.name, "Superclass must be a class.");
            }
            SimiClassImpl simiClass = (SimiClassImpl) clazz;
            if (simiClass.type == SimiClassImpl.Type.FINAL) {
              runtimeError(stmt.name, "Can't use a final class as superclass: " + simiClass.name);
            }
            superclasses.add(simiClass);
        }
      } else if (!isBaseClass) {
          superclasses = Collections.singletonList(getObjectClass());
      }
//      environment = new Environment(environment);
//      environment.define(Constants.SUPER, new SimiClassImpl.SuperClassesList(superclasses));

      Map<OverloadableFunction, SimiFunction> methods = new HashMap<>();
      Map<String, SimiProperty> constants = new HashMap<>();

      for (Expr.Assign constant : stmt.constants) {
        if (constant.annotations != null) {
          for (Stmt.Annotation annotation : constant.annotations) {
            visitAnnotationStmt(annotation);
          }
          applyAnnotations(constant);
        }
          String key = constant.name.lexeme;
          SimiProperty prop;
          if (constant.value instanceof Expr.Block) {
            SimiFunction function = new SimiFunction(new Stmt.Function(Token.named(key), (Expr.Block) constant.value, null),
                    environment, false, ((Expr.Block) constant.value).isNative, null);
            prop = new SimiValue.Callable(function, key, null);
          } else {
            prop = evaluate(constant.value);
          }
          List<SimiObject> annotations = getAnnotations(constant);
          if (prop != null) {
            constants.put(key, new SimiPropertyImpl(prop.getValue(), annotations));
          }
      }
      for (Expr mixin : stmt.mixins) {
        SimiClassImpl clazz = importClass(stmt.opener, mixin, constants::put);
        if (clazz == null) {
          runtimeError(stmt.name, "Trying to Mixin a null class!");
        }
        for (Map.Entry<OverloadableFunction, SimiFunction> method : clazz.methods.entrySet()) {
          methods.put(method.getKey(), method.getValue());
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
        Environment env = addClassesToRootEnv ? globals : environment;
        env.assign(stmt.name, classProp, false);
    }
    return classProp;
  }

  @Override
  public SimiProperty visitContinueStmt(Stmt.Continue stmt) {
    if (loopBlocks.isEmpty()) {
      runtimeError(stmt.name, "Continue outside a loop!");
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
        block.call(this, null, null, true);
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
  public SimiProperty visitElsifExpr(Expr.Elsif expr) {
    if (isTruthy(evaluate(expr.condition))) {
      return executeBlock(expr.thenBranch, this.environment, 0);
    }
    return ELSIF_FALSE;
  }

  @Override
  public SimiProperty visitIfStmt(Stmt.If stmt) {
    if (visitElsifStmt(stmt.ifstmt).getValue().getNumber().asLong() != 0) {
      return null;
    }
    for (Stmt.Elsif elsif : stmt.elsifs) {
      if (visitElsifStmt(elsif).getValue().getNumber().asLong() != 0) {
        return null;
      }
    }
    if (stmt.elseBranch != null) {
      BlockImpl elseBlock = this.environment.getOrAssignBlock(stmt, stmt.elseBranch, yieldedStmts);
      try {
        elseBlock.call(this, null, null, true);
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
  public SimiProperty visitIfExpr(Expr.If expr) {
    SimiProperty ifResult = visitElsifExpr(expr.ifstmt);
    if (ifResult != ELSIF_FALSE) {
      return ifResult;
    }
    for (Expr.Elsif elsif : expr.elsifs) {
      ifResult = visitElsifExpr(elsif);
      if (ifResult != ELSIF_FALSE) {
        return ifResult;
      }
    }
    if (expr.elseBranch != null) {
      return executeBlock(expr.elseBranch, this.environment, 0);
    }
    return null;
  }

  @Override
  public SimiProperty visitImportStmt(Stmt.Import stmt) {
    importClass(stmt.keyword, stmt.value,
            (key, value) -> environment.assign(Token.named(key), value, true));
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
    while (isTruthy(evaluate(stmt.condition))) {
      try {
        block.call(this, null, null, true);
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
    Token nextToken = new Token(TokenType.IDENTIFIER, Constants.NEXT, null, stmt.var.name.line, stmt.var.name.file);
    String nextMethodName = "#next" + block.closure.depth;
    SimiProperty nextMethod = block.closure.tryGet(nextMethodName);
    if (nextMethod == null) {
      SimiObjectImpl iterable = (SimiObjectImpl) SimiObjectImpl.getOrConvertObject(evaluate(stmt.iterable), this);
      if (iterable == null) {
        return null;
      }
      nextMethod = iterable.get(nextToken, 0, environment);
      if (nextMethod == null) {
        Token iterateToken = new Token(TokenType.IDENTIFIER, Constants.ITERATE, null, stmt.var.name.line, stmt.var.name.file);
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
        block.call(this, null, null, true);
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
      if (expr.operator.type == TokenType.DOLLAR_EQUAL) { // Mutating assignment
        Integer distance = locals.get(expr);
        if (distance != null) {
          environment.assignAt(distance, expr.name, newProp, true);
        } else {
          globals.assign(expr.name, newProp, true);
        }
      } else {
        environment.assignAt(0, expr.name, newProp);
      }
      return newProp;
    }

  @Override
  public SimiProperty visitBinaryExpr(Expr.Binary expr) {
    SimiProperty leftProp = evaluate(expr.left);
    SimiValue left = (leftProp != null) ? leftProp.getValue() : null;
    if (expr.operator.type == TokenType.QUESTION_QUESTION && left != null) { // Short-circuiting the nil coalescence operator
      return left;
    }
    SimiProperty rightProp = evaluate(expr.right);
    SimiValue right = (rightProp != null) ? rightProp.getValue() : null;

    switch (expr.operator.type) {
      case BANG_EQUAL: return new SimiValue.Number(!isEqual(left, right, expr));
      case EQUAL_EQUAL: return new SimiValue.Number(isEqual(left, right, expr));
      case LESS_GREATER: return matches(left, right, expr);
        case IS:
            return new SimiValue.Number(isInstance(left, right, expr));
        case ISNOT:
            return new SimiValue.Number(!isInstance(left, right, expr));
        case IN:
            return new SimiValue.Number(isIn(left, right, expr));
        case NOTIN:
            return new SimiValue.Number(!isIn(left, right, expr));
      case GREATER:
        if (checkNumberOperands(expr.operator, left, right)) {
          return left.getNumber().greaterThan(right.getNumber());
        } else {
          return null;
        }
      case GREATER_EQUAL:
        if (checkNumberOperands(expr.operator, left, right)) {
          return left.getNumber().greaterOrEqual(right.getNumber());
        } else {
          return null;
        }
      case LESS:
        if (checkNumberOperands(expr.operator, left, right)) {
          return left.getNumber().lessThan(right.getNumber());
        } else {
          return null;
        }
      case LESS_EQUAL:
        if (checkNumberOperands(expr.operator, left, right)) {
          return left.getNumber().lessOrEqual(right.getNumber());
        } else {
          return null;
        }
      case MINUS:
        if (checkNumberOperands(expr.operator, left, right)) {
          return left.getNumber().subtract(right.getNumber());
        } else {
          return null;
        }
      case PLUS: {
        if (left instanceof SimiValue.Number && right instanceof SimiValue.Number) {
            return left.getNumber().add(right.getNumber());
        }
        String leftStr = (left != null) ? left.toString() : "nil";
        String rightStr = (right != null) ? right.toString() : "nil";
        return new SimiValue.String(leftStr + rightStr);
      }
      case SLASH:
        if (checkNumberOperands(expr.operator, left, right)) {
          return left.getNumber().divide(right.getNumber());
        } else {
          return null;
        }
      case SLASH_SLASH:
        if (checkNumberOperands(expr.operator, left, right)) {
        return new SimiValue.Number(left.getNumber().asLong() / right.getNumber().asLong());
        } else {
          return null;
        }
      case STAR:
        if (left instanceof SimiValue.Number && right instanceof SimiValue.Number) {
          return left.getNumber().multiply(right.getNumber());
        } else if (left instanceof SimiValue.String && right instanceof SimiValue.Number && right.getNumber().isInteger()) {
          return new SimiValue.String(String.join("", Collections.nCopies(Math.toIntExact(right.getNumber().asLong()), left.getString())));
        } else {
          runtimeError(expr.operator, "\"*\" operands must be either numbers or a string and an integer!");
          return null;
        }
        case MOD:
            if (checkNumberOperands(expr.operator, left, right)) {
            return left.getNumber().mod(right.getNumber());
            } else {
              return null;
            }
      case QUESTION_QUESTION:
        return rightProp; // We already checked the condition where left is not null above.
    }

    // Unreachable.
    return null;
  }

  @Override
  public SimiProperty visitCallExpr(Expr.Call expr) {
    if (debugger != null) {
      debugger.pushCall(new Debugger.Frame(environment.deepClone(), environment, expr, null, null));
    }
    SimiProperty callee = evaluate(expr.callee);
    SimiProperty result = call(callee, expr.arguments, expr.paren);
    if (debugger != null) {
      debugger.popCall();
    }
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
      raiseNilReferenceException(paren);
      return TempNull.INSTANCE;
    } else {
      return callee;
    }

    Environment env = (paren.type == TokenType.DOLLAR_LEFT_PAREN) ? new Environment(environment) : null;

    List<SimiProperty> decomposedArgs = arguments;
    if (arguments.size() != callable.arity()) {
      // See if we can decompose argument array/object into actual arguments
      decomposedArgs = decomposeArguments(callable, arguments);
      if (decomposedArgs == arguments) { // We didn't manage to decomp args from object
        runtimeError(paren, "Expected " +
                callable.arity() + " arguments but got " +
                arguments.size() + ".");
        return null;
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
        Environment previous = environment;
        environment = new Environment(previous);
        SimiProperty result;
        if (isBaseClass) {
          String className = isBaseClass ? clazz.name : Constants.CLASS_OBJECT;
          SimiCallable nativeMethod = baseClassesNativeImpl.get(className, methodName, callable.arity());
          if (nativeMethod == null) {
            nativeMethod = baseClassesNativeImpl.get(Constants.CLASS_GLOBALS, methodName, callable.arity());
          }
          List<SimiProperty> nativeArgs = new ArrayList<>();
          nativeArgs.add(new SimiValue.Object(instance));
          nativeArgs.addAll(decomposedArgs);
          result = nativeMethod.call(this, env, nativeArgs, false);
        } else {
          result = invokeNativeCall(clazz.name, methodName, instance, decomposedArgs);
        }
        environment = previous;
        return result;
      } else {
        return invokeNativeCall(net.globulus.simi.api.Constants.GLOBALS_CLASS_NAME, methodName,
                null, decomposedArgs);
      }
    }
    return callable.call(this, env, decomposedArgs, false);
  }

  private List<SimiProperty> decomposeArguments(SimiCallable callable, List<SimiProperty> arguments) {
    if (arguments.size() == 1) {
      SimiValue value = arguments.get(0).getValue();
      if (value instanceof SimiValue.Object) {
        List<SimiValue> values = value.getObject().values();
        if (values.size() == callable.arity()) {
          return values.stream().map(v -> (SimiProperty) v).collect(Collectors.toList());
        }
      }
    }
    return arguments;
  }

  private SimiProperty invokeNativeCall(String className,
                                        String methodName,
                                        SimiObject self,
                                        List<SimiProperty> args) {
    for (NativeModulesManager manager : nativeModulesManagers) {
      try {
        SimiProperty prop = manager.call(className, methodName, self, this, args);
        if (prop != null) {
          return prop;
        }
      } catch (IllegalArgumentException ignored) {
      }
    }
    return null;
  }

  @Override
  public SimiProperty visitGetExpr(Expr.Get expr) {
    SimiProperty object = evaluate(expr.object);
    Token name = evaluateGetSetName(expr.origin, expr.name);
    if (name == null) {
      return null;
    }
    if (object instanceof TempNull) {
      raiseNilReferenceException(expr.origin);
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
        runtimeError(expr.origin,"Only instances have properties.");
        return null;
    }
  }

  private Token evaluateGetSetName(Token origin, Expr name) {
    if (name instanceof Expr.Variable) {
      return ((Expr.Variable) name).name;
    } else {
      SimiProperty prop = evaluate(name);
      SimiValue val = null;
      if (prop != null) {
        val = prop.getValue();
        if (val instanceof SimiValue.Number || val instanceof SimiValue.String) {
          String lexeme = val.getValue().toString();
          return new Token(TokenType.IDENTIFIER, lexeme, null, origin.line, origin.file); // Happy path
        }
      }
      runtimeError(origin,"Unable to parse getter/setter, invalid value: " + val + " for expr: " + name.toCode(0, true));
      return null;
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
    SimiProperty prop = evaluate(expr.expr);
    if (prop == null) {
      return null;
    }
    String string = prop.getValue().getString();
    Scanner scanner = new Scanner(FILE_RUNTIME, string + "\n", debugger);
    Parser parser = new Parser(scanner.scanTokens(true), debugger);
    for (Stmt stmt : parser.parse()) {
        if (stmt instanceof Stmt.Annotation) {
            visitAnnotationStmt((Stmt.Annotation) stmt);
        } else if (stmt instanceof Stmt.Class) {
            return visitClassStmt((Stmt.Class) stmt, false);
        } else if (stmt instanceof Stmt.Expression) {
            return evaluate(((Stmt.Expression) stmt).expression);
        } else {
            ErrorHub.sharedInstance().error(Constants.EXCEPTION_INTERPRETER, FILE_RUNTIME,0,
                    "Invalid GU expression!\n" + expr.toCode(0, true)
                            + "\nevaluates to: " + string);
            return null;
        }
    }
    return null;
  }

  @Override
  public SimiProperty visitIvicExpr(Expr.Ivic expr) {
    SimiProperty prop = evaluate(expr.expr);
    String code;
    if (prop == null) {
      code = TempNull.INSTANCE.toCode(0, false);
    } else {
      code = prop.getValue().toCode(0, false)
              .replace("end\n,", "end,")
              .replace("end\n)", "end)");
    }
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
      runtimeError(expr.origin, "Only objects have fields.");
      return null;
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
//    int distance = locals.get(expr);
    SimiMethod method = null;
    List<SimiClassImpl> superclasses = ((SimiClassImpl.SuperClassesList) environment.get(Token.superToken()).getValue()).value;
    if (expr.superclass != null) {
      superclasses = superclasses.stream()
              .filter(superclass -> superclass.name.equals(expr.superclass.lexeme)).collect(Collectors.toList());
      if (superclasses.isEmpty()) {
        runtimeError(expr.superclass, "Invalid superclass specified!");
        return null;
      }
    }

    // "self" is always one level nearer than "super"'s environment.
    SimiObjectImpl object = (SimiObjectImpl) environment.get(Token.self()).getValue().getObject();

    for (SimiClassImpl superclass : superclasses) {
      method = superclass.findMethod(object, expr.method.lexeme, expr.arity);
      if (method != null) {
        break;
      }
    }

    if (method == null) {
      runtimeError(expr.method, "Undefined property '" + expr.method.lexeme + "'.");
      return null;
    }

    return new SimiValue.Callable(method, expr.method.lexeme, object);
  }

  @Override
  public SimiProperty visitSelfExpr(Expr.Self expr) {
    if (expr.specifier != null) {
      return lookUpVariable(expr.specifier, expr);
    }
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
        if (checkNumberOperand(expr.operator, right)) {
          return right.getNumber().negate();
        } else {
          return null;
        }
      case QUESTION:
        return (right == null) ? TempNull.INSTANCE : right;
      case BANG_BANG: {
        if (rightProp == null || rightProp.getAnnotations() == null) {
          return null;
        }
        ArrayList<SimiValue> annotations = rightProp.getAnnotations().stream()
                .map(SimiValue.Object::new)
                .collect(Collectors.toCollection(ArrayList::new));
        return new SimiValue.Object(SimiObjectImpl.fromArray(getObjectClass(), true, annotations));
      }
    }
    // Unreachable.
    return null;
  }

  @Override
  public SimiProperty visitVariableExpr(Expr.Variable expr) {
    SimiProperty prop = lookUpVariable(expr.name, expr);
    if (prop == null && expr.backupSelfGet != null) {
      prop = visitGetExpr(expr.backupSelfGet);
    }
    return prop;
  }

    @Override
    public SimiValue visitObjectLiteralExpr(Expr.ObjectLiteral expr) {
        boolean immutable = (expr.opener.type == TokenType.LEFT_BRACKET);
        SimiClassImpl objectClass = getObjectClass();
        SimiObjectImpl object;
        LinkedHashMap<String, SimiProperty> mapFields = new LinkedHashMap<>();
        ArrayList<SimiProperty> arrayFields = new ArrayList<>();
        for (Expr propExpr : expr.props) {
          String key;
          Expr valueExpr;
          if (propExpr instanceof Expr.Assign) {
            Expr.Assign assign = (Expr.Assign) propExpr;
            key = assign.name.lexeme;
            if (key == null) {
              key = assign.name.literal.getString();
            }
            valueExpr = assign.value;
          } else {
            key = null;
            valueExpr = propExpr;
          }
          SimiProperty prop;
          if (valueExpr instanceof Expr.Block) {
            prop = new SimiValue.Callable(new BlockImpl((Expr.Block) valueExpr, environment), key, null);
          } else {
            prop = evaluate(valueExpr);
          }
          if (key != null
                  && key.equals(TokenType.CLASS.toCode())
                  && prop.getValue() instanceof SimiValue.Object
                  && prop.getValue().getObject() instanceof SimiClassImpl) {
            objectClass = (SimiClassImpl) prop.getValue().getObject();
          } else {
            if (key != null) {
              mapFields.put(key, prop);
            } else {
              arrayFields.add(prop);
            }
          }
        }
        object = new SimiObjectImpl(objectClass, immutable, mapFields, arrayFields);
        for (SimiValue value : object.values()) {
          if (value instanceof SimiValue.Callable) {
            ((SimiValue.Callable) value).bind(object);
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

  private boolean checkNumberOperand(Token operator, SimiValue operand) {
    if (operand instanceof SimiValue.Number) {
      return true;
    }
    runtimeError(operator, "Operand must be a number.");
    return false;
  }

  private boolean checkNumberOperands(Token operator, SimiValue left, SimiValue right) {
    if (left instanceof SimiValue.Number && right instanceof SimiValue.Number) {
      return true;
    }
    runtimeError(operator, "Operands must be numbers.");
    return false;
  }

  static boolean isTruthy(SimiProperty object) {
    if (object == null || object == TempNull.INSTANCE
            || object.getValue() == null || object.getValue() == TempNull.INSTANCE) {
        return false;
    }
    try {
        long value = object.getValue().getNumber().asLong();
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
      Token equals = new Token(TokenType.IDENTIFIER, Constants.EQUALS, null, expr.operator.line, expr.operator.file);
      return call(((SimiObjectImpl) a.getObject()).get(equals, 1, environment).getValue(),
              equals, Collections.singletonList(b)).getValue().getNumber().asLong() != 0;
    }
    return a.equals(b);
  }

  private SimiValue matches(SimiValue a, SimiValue b, Expr.Binary expr) {
    // nil is only equal to nil.
    if (a == null && b == null) {
      return SimiValue.Number.TRUE;
    }
    if (a == null) {
      return SimiValue.Number.FALSE;
    }
    if (a instanceof SimiValue.Object) {
      Token matches = new Token(TokenType.IDENTIFIER, Constants.MATCHES, null, expr.operator.line, expr.operator.file);
      return call(((SimiObjectImpl) a.getObject()).get(matches, 1, environment).getValue(), matches, Arrays.asList(b)).getValue();
    }
    return new SimiValue.Number(a.compareTo(b));
  }

  private boolean isInstance(SimiValue a, SimiValue b, Expr.Binary expr) {
    SimiObject right = SimiObjectImpl.getOrConvertObject(b, this);

    // is Function
    if (a instanceof SimiValue.Callable
            && right instanceof SimiClassImpl
            && ((SimiClassImpl) right).name.equals(Constants.CLASS_FUNCTION)) {
      return true;
    }

    // is Class
    if (a instanceof SimiValue.Object
            && a.getObject() instanceof SimiClassImpl
            && expr.right instanceof Expr.Variable
            && ((Expr.Variable) expr.right).name.lexeme.equals(Constants.CLASS_CLASS)) {
        return true;
    }

    SimiObject left = SimiObjectImpl.getOrConvertObject(a, this);
    if (left == null || right == null) {
      return false;
    }
    SimiClassImpl clazz = (SimiClassImpl) right;
    if (left instanceof SimiObjectImpl) {
      return ((SimiObjectImpl) left).is(clazz);
    } else if (left instanceof SimiException) {
      return ((SimiClassImpl) right).name.equals(((SimiClassImpl) left.getSimiClass()).name);
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
//          runtimeError(expr.operator, "Right side must be an Object!");
      }
      if (object == null) {
        return false;
      }
      Token has = new Token(TokenType.IDENTIFIER, Constants.HAS, null, expr.operator.line, expr.operator.file);
      SimiProperty p = call(object.get(has, 1, environment).getValue(), has, Collections.singletonList(a));
      return p.getValue().getNumber().asLong() != 0;
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
            || className.equals(Constants.CLASS_EXCEPTION)
            || className.equals(Constants.CLASS_DEBUGGER);
  }

  private SimiClassImpl getObjectClass() {
    SimiProperty classObj = globals.tryGet(Constants.CLASS_OBJECT);
    if (classObj != null) {
      return (SimiClassImpl) classObj.getValue().getObject();
    } else {
      return null;
    }
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

  private SimiClassImpl importClass(Token keyword, Expr expr, ClassImporter classImporter) {
      SimiProperty value = evaluate(expr);
      if (value == null) {
          return null;
      }
    SimiObject object = SimiObjectImpl.getOrConvertObject(value.getValue(), this);
    if (!(object instanceof SimiClassImpl)) {
      runtimeError(keyword, "Import or mixin statement must be followed by an identifier that resolves to a class!");
      return null;
    }
    SimiClassImpl clazz = (SimiClassImpl) object;
    Set<Map.Entry<String, SimiProperty>> fields = new HashSet<>(clazz.fields.entrySet());
    for (Map.Entry<String, SimiProperty> entry : fields) {
      String key = entry.getKey();
      if (!key.equals(Constants.INIT) && !key.startsWith(Constants.PRIVATE)) {
        classImporter.importValue(key, entry.getValue());
      }
    }
    return clazz;
  }

  private void raiseNilReferenceException(Token token) {
    String message = "Nil reference found at " + token.file + " line " + token.line + ": " + token.toString();
    raiseException(new SimiException(null, (SimiClass) environment.tryGet(Constants.EXCEPTION_NIL_REFERENCE).getValue().getObject(), message));
  }
  
  private void runtimeError(Token token, String message) {
    ErrorHub.sharedInstance().error(Constants.EXCEPTION_INTERPRETER, token, message);
  }

  // Debugger.Evaluator

    @Override
    public String eval(String input, Environment environment) {
        Environment currentEnv = this.environment;
        this.environment = environment;
        Scanner scanner = new Scanner(FILE_RUNTIME, input + "\n", null);
        Parser parser = new Parser(scanner.scanTokens(true), null);
        SimiProperty result = execute(parser.parse().get(0));
        String text = (result != null) ? result.toString() : TempNull.INSTANCE.toCode(0, true);
        this.environment = currentEnv;
        return text;
    }

    @Override
    public Environment getGlobalEnvironment() {
        return globals;
    }

  // /Debugger.Evaluator

  @FunctionalInterface
  private interface ClassImporter {
    void importValue(String key, SimiProperty prop);
  }

  private final static SimiProperty ELSIF_FALSE = new SimiProperty() {
    @Override
    public SimiValue getValue() {
      return null;
    }

    @Override
    public void setValue(SimiValue value) {

    }

    @Override
    public List<SimiObject> getAnnotations() {
      return null;
    }

    @Override
    public SimiProperty clone(boolean mutable) {
      return null;
    }
  };
}
