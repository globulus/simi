package net.globulus.simi;

import net.globulus.simi.api.*;

import java.util.List;

class SimiFunction implements SimiCallable {

  Stmt.Function declaration;
  private final BlockImpl block;
  private final boolean isInitializer;
  public final boolean isNative;
  final List<SimiObject> annotations;

  SimiFunction(Stmt.Function declaration,
               Environment closure,
               boolean isInitializer,
               boolean isNative,
               List<SimiObject> annotations) {
    this.declaration = declaration;
    this.block = new BlockImpl(declaration.block, closure);
    this.isInitializer = isInitializer;
    this.isNative = isNative;
    this.annotations = annotations;
  }

  private SimiFunction(SimiFunction original, BlockImpl block) {
      this.declaration = original.declaration;
      this.block = block;
      this.isInitializer = original.isInitializer;
      this.isNative = original.isNative;
      this.annotations = original.annotations;
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
  public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
    SimiProperty prop = block.call(interpreter, arguments, rethrow);
    if (isInitializer) {
        return block.closure.getAt(0, Constants.SELF);
    }
    return prop;
  }

  @Override
  public String toCode() {
    return declaration.toCode();
  }
}
