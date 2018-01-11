package net.globulus.simi;

import net.globulus.simi.api.SimiValue;

class Return extends RuntimeException {
  final SimiValue value;

  Return(SimiValue value) {
    super(null, null, false, false);
    this.value = value;
  }
}
