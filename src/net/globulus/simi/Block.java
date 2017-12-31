package net.globulus.simi;

import net.globulus.simi.api.SimiCallable;
import net.globulus.simi.api.SimiInterpreter;

import java.util.List;

class Block extends Value<Expr.Block, Block> implements SimiCallable {

  final Environment closure;

  Block(Expr.Block declaration, Environment closure) {
    super(declaration);
    this.closure = closure;
  }

  @Override
  Block bind(SimiObjectImpl instance) {
    Environment environment = new Environment(closure);
    environment.assign(Token.self(), instance);
    return new Block(declaration, environment);
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
  public Object call(SimiInterpreter interpreter, List<Object> arguments, boolean immutable) {
    Environment environment = new Environment(closure);
    for (int i = 0; i < declaration.params.size(); i++) {
      environment.define(declaration.params.get(i).lexeme,
              arguments.get(i));
    }

    try {
      interpreter.executeBlock(declaration, environment);
    } catch (Return returnValue) {
      return returnValue.value;
    }

    return null;
  }
}
