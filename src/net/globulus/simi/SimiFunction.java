package net.globulus.simi;

import java.util.List;

class SimiFunction extends SimiValue<Stmt.Function, SimiFunction> implements SimiCallable {

  private final SimiBlock block;
  private final boolean isInitializer;
  private final boolean isNative;

  SimiFunction(Stmt.Function declaration,
               Environment closure,
               boolean isInitializer,
               boolean isNative) {
    super(declaration);
    this.block = new SimiBlock(declaration.block, closure);
    this.isInitializer = isInitializer;
    this.isNative = isNative;
  }

  private SimiFunction(Stmt.Function declaration,
                       SimiBlock block,
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
  public Object call(Interpreter interpreter, List<Object> arguments, boolean immutable) {
    block.call(interpreter, arguments, immutable);
    if (isInitializer) {
        return block.closure.getAt(0, Constants.SELF);
    }
    return null;
  }
}
