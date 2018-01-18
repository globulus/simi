package net.globulus.simi;

import net.globulus.simi.api.BlockInterpreter;
import net.globulus.simi.api.SimiCallable;
import net.globulus.simi.api.SimiValue;

import java.util.List;

class SimiFunction implements SimiCallable {

  Stmt.Function declaration;
  private final BlockImpl block;
  private final boolean isInitializer;
  public final boolean isNative;

  SimiFunction(Stmt.Function declaration,
               Environment closure,
               boolean isInitializer,
               boolean isNative) {
    this.declaration = declaration;
    this.block = new BlockImpl(declaration.block, closure);
    this.isInitializer = isInitializer;
    this.isNative = isNative;
  }

  private SimiFunction(SimiFunction original, BlockImpl block) {
      this.declaration = original.declaration;
      this.block = block;
      this.isInitializer = original.isInitializer;
      this.isNative = original.isNative;
  }

  SimiFunction bind(SimiObjectImpl instance) {
//      block.bind(instance);
      return new SimiFunction(this, block.bind(instance));
  }

  @Override
  public String toString() {
    return "<" + (isNative ? "native" : "def") + " " + declaration.name.lexeme + ">";
  }

  @Override
  public int arity() {
    return block.arity();
  }

  @Override
  public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments, boolean rethrow) {
    SimiValue value = block.call(interpreter, arguments, rethrow);
    if (isInitializer) {
        return block.closure.getAt(0, Constants.SELF);
    }
    return value;
  }
}
