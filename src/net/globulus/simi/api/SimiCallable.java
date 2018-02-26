package net.globulus.simi.api;

import java.util.List;

public interface SimiCallable {
  int arity();
  SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow);
}
