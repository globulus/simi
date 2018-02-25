package net.globulus.simi;

import net.globulus.simi.api.*;

import java.util.List;

class BlockImpl implements SimiBlock, SimiCallable {

  final Expr.Block declaration;
  final Environment closure;

  private Integer lastStatement;
  private Environment lastClosure;

  BlockImpl(Expr.Block declaration, Environment closure) {
    this.declaration = declaration;
    this.closure = closure;
  }

  BlockImpl bind(SimiObjectImpl instance) {
    Environment environment = new Environment(closure);
    environment.assign(Token.self(), new SimiValue.Object(instance), null, false);
    return new BlockImpl(declaration, environment);
  }

  boolean isNative() {
    return declaration.isNative();
  }

  @Override
  public void yield(int index) {
    this.lastStatement = index;
    this.lastClosure = this.closure;
  }

  private void clearYield() {
    this.lastStatement = null;
    this.lastClosure = null;
  }

  @Override
  public String toString() {
    return "<block>";
  }

  @Override
  public int arity() {
    return declaration.params.size();
  }

  @Override
  public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments, boolean rethrow) {
    Environment environment = new Environment(lastClosure != null ? lastClosure : closure);
    if (arguments != null) {
      for (int i = 0; i < declaration.params.size(); i++) {
        environment.define(declaration.params.get(i).lexeme, arguments.get(i));
      }
    }

    try {
      interpreter.executeBlock(this, environment, (lastStatement != null) ? lastStatement : 0);
    } catch (Return returnValue) {
      clearYield();
      if (rethrow) {
        throw returnValue;
      } else {
        return returnValue.value;
      }
    } catch (Yield yield) {
      if (rethrow) {
        throw new Yield(yield.value, true);
      } else {
        return yield.value;
      }
    }
    clearYield();
    return null;
  }

  @Override
  public List<? extends SimiStatement> getStatements() {
    return declaration.getStatements();
  }
}
