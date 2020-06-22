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

  public static Token copying(Token other, TokenType newType) {
    return new Token(newType, other.lexeme, other.literal, other.line, other.file);
  }

  static Token ofType(TokenType type) {
    return new Token(type, null, null, 0, null);
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

  public static class Factory {
    final Token opener;
    public Factory(Token opener) {
      this.opener = opener;
    }

    public Token ofType(TokenType type) {
      return new Token(type, null, null, opener.line, opener.file);
    }

    public Token self() {
      return new Token(TokenType.SELF, Constants.SELF, null, opener.line, opener.file);
    }

    public Token superToken() {
      return new Token(TokenType.SUPER, Constants.SUPER, null, opener.line, opener.file);
    }

    public Token selfDef() {
      return new Token(TokenType.DEF, Constants.SELF_DEF, null, opener.line, opener.file);
    }

    public Token nativeCall(String name) {
      return new Token(TokenType.DEF, name, null, opener.line, opener.file);
    }

    public Token named(String name) {
      return new Token(TokenType.IDENTIFIER, name, null, opener.line, opener.file);
    }

    public Token ofString(String value) {
      return new Token(TokenType.STRING, value, new SimiValue.String(value), opener.line, opener.file);
    }

    public Token ofLong(Long value) {
      return new Token(TokenType.NUMBER, "" + value, new SimiValue.Number(value), opener.line, opener.file);
    }
  }
}
