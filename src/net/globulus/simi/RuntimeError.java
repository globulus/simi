//> Evaluating Expressions runtime-error-class
package net.globulus.simi;

class RuntimeError extends RuntimeException {
  final Token token;

  RuntimeError(Token token, String message) {
    super(message);
    this.token = token;
  }
}
