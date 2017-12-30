package net.globulus.simi;

import java.util.List;

class SimiBlock extends SimiValue<Expr.Block, SimiBlock> implements SimiCallable {

  final Environment closure;

  SimiBlock(Expr.Block declaration, Environment closure) {
    super(declaration);
    this.closure = closure;
  }

  @Override
  SimiBlock bind(SimiObjectImpl instance) {
    Environment environment = new Environment(closure);
    environment.assign(Token.self(), instance);
    return new SimiBlock(declaration, environment);
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
  public Object call(Interpreter interpreter, List<Object> arguments, boolean immutable) {
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
