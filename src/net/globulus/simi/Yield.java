package net.globulus.simi;

class Yield extends RuntimeException {

  final Object value;
  final boolean rethrown;

  Yield(Object value, boolean rethrown) {
    super(null, null, false, false);
    this.value = value;
    this.rethrown = rethrown;
  }
}
