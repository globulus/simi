package net.globulus.simi;

import net.globulus.simi.api.SimiCallable;
import net.globulus.simi.api.SimiInterpreter;

import java.util.List;

class SimiFunction extends Value<Stmt.Function, SimiFunction> implements SimiCallable {

  private final Block block;
  private final boolean isInitializer;
  private final boolean isNative;

  SimiFunction(Stmt.Function declaration,
               Environment closure,
               boolean isInitializer,
               boolean isNative) {
    super(declaration);
    this.block = new Block(declaration.block, closure);
    this.isInitializer = isInitializer;
    this.isNative = isNative;
  }

  private SimiFunction(Stmt.Function declaration,
                       Block block,
                       boolean isInitializer,
                       boolean isNative) {
      super(declaration);
      this.block = block;
      this.isInitializer = isInitializer;
      this.isNative = isNative;
  }

  @Override
  SimiFunction bind(SimiObjectImpl instance) {
      block.bind(instance);
      return new SimiFunction(declaration, block, isInitializer, isNative);
  }

  @Override
  public String toString() {
    return "<fn " + declaration.name.lexeme + ">";
  }

  @Override
  public int arity() {
    return block.arity();
  }

  @Override
  public Object call(SimiInterpreter interpreter, List<Object> arguments, boolean immutable) {
    block.call(interpreter, arguments, immutable);
    if (isInitializer) {
        return block.closure.getAt(0, Constants.SELF);
    }
    return null;
  }
}
