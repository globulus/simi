package net.globulus.simi;

import net.globulus.simi.api.SimiValue;

class Token {

  final TokenType type;
  final String lexeme;
  final SimiValue literal;
  final int line;
  final String file;

  boolean hasBreakpoint = false;

  Token(TokenType type, String lexeme, SimiValue literal, int line, String file) {
    this.type = type;
    this.lexeme = lexeme;
    this.literal = literal;
    this.line = line;
    this.file = file;
  }

  static Token self() {
    return new Token(TokenType.SELF, Constants.SELF, null, 0, null);
  }

  static Token superToken() {
    return new Token(TokenType.SUPER, Constants.SUPER, null, 0, null);
  }

  static Token selfDef() {
    return new Token(TokenType.DEF, Constants.SELF_DEF, null, 0, null);
  }

  static Token nativeCall(String name) {
    return new Token(TokenType.DEF, name, null, 0, null);
  }

  static Token named(String name) {
    return new Token(TokenType.IDENTIFIER, name, null, 0, null);
  }

  @Override
  public String toString() {
    return type + " " + lexeme + " " + literal;
  }
}
