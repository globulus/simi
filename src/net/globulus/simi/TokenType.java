package net.globulus.simi;

import net.globulus.simi.api.Codifiable;

public enum TokenType implements Codifiable {
  // Single-character tokens.
  LEFT_PAREN, RIGHT_PAREN,
  LEFT_BRACKET, RIGHT_BRACKET,
  LEFT_BRACE, RIGHT_BRACE,
  COMMA,
  DOT, DOT_DOT, DOT_DOT_DOT,
  COLON, NEWLINE,

  // One or two character tokens.
  BANG, BANG_BANG, BANG_EQUAL,
  EQUAL, EQUAL_EQUAL,
  GREATER, GREATER_EQUAL,
  LESS, LESS_EQUAL,
  LESS_GREATER,
    PLUS, PLUS_EQUAL,
    MINUS, MINUS_EQUAL,
  STAR, STAR_STAR, STAR_EQUAL,
    SLASH, SLASH_SLASH, SLASH_EQUAL, SLASH_SLASH_EQUAL,
  MOD, MOD_MOD, MOD_EQUAL,
  DOLLAR_LEFT_PAREN, DOLLAR_LEFT_BRACKET, DOLLAR_EQUAL,
  QUESTION, QUESTION_QUESTION, QUESTION_QUESTION_EQUAL,

  // Literals.
  IDENTIFIER, STRING, NUMBER,

  // Keywords.
  AND, BREAK, CLASS, CLASS_FINAL, CLASS_OPEN, CONTINUE, ELSE,
  FALSE, DEF, DO, FOR, RESCUE, IF, NIL, OR, PRINT, RETURN, SUPER,
  SELF, TRUE, WHILE, PASS, IN, IS, NOT, ELSIF, ISNOT,
  NOTIN, NATIVE, IMPORT, YIELD, WHEN,

  GU, IVIC,

  EOF;

  public String toCode() {
    switch (this) {
      case LEFT_PAREN:
        return "(";
      case RIGHT_PAREN:
        return ")";
      case LEFT_BRACKET:
        return "[";
      case RIGHT_BRACKET:
        return "]";
      case LEFT_BRACE:
        return "{";
      case RIGHT_BRACE:
        return "}";
      case COMMA:
        return ",";
      case DOT:
        return ".";
      case COLON:
        return ":";
      case NEWLINE:
        return "\n";
      case BANG:
        return "!";
      case BANG_BANG:
        return "!!";
      case BANG_EQUAL:
        return "!=";
      case EQUAL:
        return "=";
      case EQUAL_EQUAL:
        return "==";
      case GREATER:
        return ">";
      case GREATER_EQUAL:
        return ">=";
      case LESS:
        return "<";
      case LESS_EQUAL:
        return "<=";
      case LESS_GREATER:
        return "<>";
      case PLUS:
        return "+";
      case PLUS_EQUAL:
        return "+=";
      case MINUS:
        return "-";
      case MINUS_EQUAL:
        return "-=";
      case STAR:
        return "*";
      case STAR_STAR:
        return "**";
      case STAR_EQUAL:
        return "*=";
      case SLASH:
        return "/";
      case SLASH_SLASH:
        return "//";
      case SLASH_EQUAL:
        return "/=";
      case SLASH_SLASH_EQUAL:
        return "//=";
      case MOD:
        return "%";
      case MOD_MOD:
        return "%%";
      case MOD_EQUAL:
        return "%=";
      case DOLLAR_LEFT_PAREN:
        return "$(";
      case DOLLAR_LEFT_BRACKET:
        return "$[";
      case DOLLAR_EQUAL:
        return "$=";
      case QUESTION:
        return "?";
      case QUESTION_QUESTION:
        return "??";
      case QUESTION_QUESTION_EQUAL:
        return "??=";
      case IDENTIFIER:
      case STRING:
      case NUMBER:
      case EOF:
        throw new IllegalArgumentException("Shouldn't be used with toCode()");
      case ISNOT:
        return "is not";
      case NOTIN:
        return "not in";
      case CLASS_FINAL:
        return "class_";
      case CLASS_OPEN:
        return "class$";
      default:
        return toString().toLowerCase();
    }
  }

  @Override
  public String toCode(int indentationLevel, boolean ignoreFirst) {
    if (ignoreFirst) {
      return toCode();
    }
    return Codifiable.getIndentation(indentationLevel) + toCode();
  }

  @Override
  public int getLineNumber() {
    return -1;
  }

  @Override
  public String getFileName() {
    return null;
  }

  @Override
  public boolean hasBreakPoint() {
    return false;
  }
}
