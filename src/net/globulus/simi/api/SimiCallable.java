package net.globulus.simi.api;

import java.util.List;

public interface SimiCallable {
  int arity();
  SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments, boolean rethrow);
}
