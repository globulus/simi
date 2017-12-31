package net.globulus.simi.api;

import java.util.List;

public interface SimiCallable {
  int arity();
  Object call(SimiInterpreter interpreter, List<Object> arguments, boolean immutable);
}
