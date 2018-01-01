package net.globulus.simi;

import net.globulus.simi.api.SimiValue;

class Token {

  final TokenType type;
  final String lexeme;
  final SimiValue literal;
  final int line; // [location]

  Token(TokenType type, String lexeme, SimiValue literal, int line) {
    this.type = type;
    this.lexeme = lexeme;
    this.literal = literal;
    this.line = line;
  }

  static Token self() {
    return new Token(TokenType.SELF, Constants.SELF, null, 0);
  }

  static Token nativeCall(String name) {
    return new Token(TokenType.NATIVE, name, null, 0);
  }

  @Override
  public String toString() {
    return type + " " + lexeme + " " + literal;
  }
}
