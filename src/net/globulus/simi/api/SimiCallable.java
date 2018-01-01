package net.globulus.simi.api;

import java.util.List;

public interface SimiCallable {
  int arity();
  Object call(BlockInterpreter interpreter, List<SimiValue> arguments, boolean immutable);
}
