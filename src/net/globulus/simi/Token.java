package net.globulus.simi;

class Token {

  final TokenType type;
  final String lexeme;
  final Object literal;
  final int line; // [location]

  Token(TokenType type, String lexeme, Object literal, int line) {
    this.type = type;
    this.lexeme = lexeme;
    this.literal = literal;
    this.line = line;
  }

  static Token self() {
    return new Token(TokenType.SELF, Constants.SELF, null, 0);
  }

  @Override
  public String toString() {
    return type + " " + lexeme + " " + literal;
  }
}
