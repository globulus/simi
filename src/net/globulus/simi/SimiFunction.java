package net.globulus.simi;

import net.globulus.simi.api.SimiCallable;
import net.globulus.simi.api.BlockInterpreter;
import net.globulus.simi.api.SimiObject;
import net.globulus.simi.api.SimiValue;

import java.util.List;

class SimiFunction implements SimiCallable {

  private Stmt.Function declaration;
  private final BlockImpl block;
  private final boolean isInitializer;
  public final boolean isNative;
  private final SimiObject instance;

  SimiFunction(Stmt.Function declaration,
               Environment closure,
               boolean isInitializer,
               boolean isNative) {
    this.declaration = declaration;
    this.block = new BlockImpl(declaration.block, closure);
    this.isInitializer = isInitializer;
    this.isNative = isNative;
    this.instance = null;
  }

  private SimiFunction(Stmt.Function declaration,
                       BlockImpl block,
                       boolean isInitializer,
                       boolean isNative,
                       SimiObject instance) {
      this.declaration = declaration;
      this.block = block;
      this.isInitializer = isInitializer;
      this.isNative = isNative;
      this.instance = instance;
  }

  SimiFunction bind(SimiObjectImpl instance) {
//      block.bind(instance);
      return new SimiFunction(declaration, block.bind(instance), isInitializer, isNative, instance);
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
  public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments) {
    SimiValue value = block.call(interpreter, arguments);
    if (isInitializer) {
        return block.closure.getAt(0, Constants.SELF);
    }
    return value;
  }
}
