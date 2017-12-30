package net.globulus.simi;

import java.util.List;

interface SimiCallable {
  int arity();
  Object call(Interpreter interpreter, List<Object> arguments, boolean immutable);
}
