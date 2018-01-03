package net.globulus.simi;

import net.globulus.simi.api.*;

import java.util.List;

class BlockImpl implements SimiBlock, SimiCallable {

  final Expr.Block declaration;
  final Environment closure;

  BlockImpl(Expr.Block declaration, Environment closure) {
    this.declaration = declaration;
    this.closure = closure;
  }

  BlockImpl bind(SimiObjectImpl instance) {
    Environment environment = new Environment(closure);
    environment.assign(Token.self(), new SimiValue.Object(instance), false);
    return new BlockImpl(declaration, environment);
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
  public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
    Environment environment = new Environment(closure);
    for (int i = 0; i < declaration.params.size(); i++) {
      environment.define(declaration.params.get(i).lexeme, arguments.get(i));
    }

    try {
      interpreter.executeBlock(declaration, environment);
    } catch (Return returnValue) {
      return returnValue.value;
    }

    return null;
  }

  @Override
  public List<? extends SimiStatement> getStatements() {
    return declaration.getStatements();
  }
}
