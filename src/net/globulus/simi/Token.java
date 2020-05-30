package net.globulus.simi;

import net.globulus.simi.api.SimiValue;

public class Token {

  public final TokenType type;
  public final String lexeme;
  public final SimiValue literal;
  public final int line;
  public final String file;

  public boolean hasBreakpoint = false;

  public Token(TokenType type, String lexeme, SimiValue literal, int line, String file) {
    this.type = type;
    this.lexeme = lexeme;
    this.literal = literal;
    this.line = line;
    this.file = file;
  }

  public static Token ofType(TokenType type) {
    return new Token(type, null, null, 0, null);
  }

  public static Token self() {
    return new Token(TokenType.SELF, Constants.SELF, null, 0, null);
  }

  public static Token superToken() {
    return new Token(TokenType.SUPER, Constants.SUPER, null, 0, null);
  }

  public static Token selfDef() {
    return new Token(TokenType.DEF, Constants.SELF_DEF, null, 0, null);
  }

  public static Token nativeCall(String name) {
    return new Token(TokenType.DEF, name, null, 0, null);
  }

  public static Token named(String name) {
    return new Token(TokenType.IDENTIFIER, name, null, 0, null);
  }

  public static Token copying(Token other, TokenType newType) {
    return new Token(newType, other.lexeme, other.literal, other.line, other.file);
  }

  public static Token ofString(String value) {
    return new Token(TokenType.STRING, value, new SimiValue.String(value), 0, null);
  }

  @Override
  public String toString() {
    return type + " " + lexeme + " " + literal;
  }
}
