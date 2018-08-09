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

  static Token superToken() {
    return new Token(TokenType.SUPER, Constants.SUPER, null, 0);
  }

  static Token selfDef() {
    return new Token(TokenType.DEF, Constants.SELF_DEF, null, 0);
  }

  static Token nativeCall(String name) {
    return new Token(TokenType.DEF, name, null, 0);
  }

  static Token named(String name) {
    return new Token(TokenType.IDENTIFIER, name, null, 0);
  }

  @Override
  public String toString() {
    return type + " " + lexeme + " " + literal;
  }
}
