package net.globulus.simi;

import net.globulus.simi.api.SimiValue;

class Yield extends RuntimeException {

  final SimiValue value;
  final boolean rethrown;

  Yield(SimiValue value, boolean rethrown) {
    super(null, null, false, false);
    this.value = value;
    this.rethrown = rethrown;
  }
}
