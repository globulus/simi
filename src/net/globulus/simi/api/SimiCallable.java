package net.globulus.simi.api;

import java.util.List;

public interface SimiCallable extends Codifiable {

  int arity();
  SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow);

  default String toCode(int indentationLevel, boolean ignoreFirst) {
    return null;
  }
  default int getLineNumber() {
    return -1;
  }
  default String getFileName() { return null; }
  default boolean hasBreakPoint() {
    return false;
  }
}
