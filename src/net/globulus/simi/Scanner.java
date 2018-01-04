package net.globulus.simi;

import net.globulus.simi.api.SimiValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Scanner {

  private static final Map<String, TokenType> keywords;

  static {
    keywords = new HashMap<>();
    keywords.put("and",    TokenType.AND);
    keywords.put("class",  TokenType.CLASS);
    keywords.put("def",    TokenType.DEF);
    keywords.put("end",    TokenType.END);
    keywords.put("else",   TokenType.ELSE);
    keywords.put("elsif",  TokenType.ELSIF);
    keywords.put("false",  TokenType.FALSE);
    keywords.put("for",    TokenType.FOR);
    keywords.put("gu",     TokenType.GU);
    keywords.put("if",     TokenType.IF);
    keywords.put("import",     TokenType.IMPORT);
    keywords.put("in",     TokenType.IN);
    keywords.put("is",     TokenType.IS);
    keywords.put("native",      TokenType.NATIVE);
    keywords.put("nil",    TokenType.NIL);
    keywords.put("not",    TokenType.NOT);
    keywords.put("or",     TokenType.OR);
    keywords.put("pass",    TokenType.PASS);
    keywords.put("print",          TokenType.PRINT);
    keywords.put("return",         TokenType.RETURN);
    keywords.put(Constants.SELF,   TokenType.SELF);
    keywords.put(Constants.SUPER,  TokenType.SUPER);
    keywords.put("true",   TokenType.TRUE);
    keywords.put("while",  TokenType.WHILE);
  }
  private final String source;
  private final List<Token> tokens = new ArrayList<>();
//> scan-state
  private int start = 0;
  private int current = 0;
  private int line = 1;

  Scanner(String source) {
    this.source = source;
  }
  List<Token> scanTokens(boolean addEof) {
    while (!isAtEnd()) {
      // We are at the beginning of the next lexeme.
      start = current;
      scanToken();
    }

    if (addEof) {
      tokens.add(new Token(TokenType.EOF, "", null, line));
    }
    return tokens;
  }

  private void scanToken() {
    char c = advance();
    switch (c) {
      case '(': addToken(TokenType.LEFT_PAREN); break;
      case ')': addToken(TokenType.RIGHT_PAREN); break;
      case '[': addToken(TokenType.LEFT_BRACKET); break;
      case ']': addToken(TokenType.RIGHT_BRACKET); break;
      case ',': addToken(TokenType.COMMA); break;
      case '.': addToken(TokenType.DOT); break;
      case ':': addToken(TokenType.COLON); break;
//> two-char-tokens
      case '!': addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG); break;
      case '=': addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL); break;
      case '<': addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS); break;
      case '>': addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER); break;
      case '+': addToken(match('=') ? TokenType.PLUS_EQUAL : TokenType.PLUS); break;
      case '-': addToken(match('=') ? TokenType.MINUS_EQUAL : TokenType.MINUS); break;
      case '/': addToken(match('=') ? TokenType.SLASH_EQUAL : TokenType.SLASH); break;
      case '*': {
        if (match('*')) {
          addToken(TokenType.STAR_STAR);
        } else if (match('=')) {
          addToken(TokenType.STAR_EQUAL);
        } else {
          addToken(TokenType.STAR);
        }
      } break;
      case '%': {
        if (match('%')) {
          addToken(TokenType.MOD_MOD);
        } else if (match('=')) {
          addToken(TokenType.MOD_EQUAL);
        } else {
          addToken(TokenType.MOD);
        }
      } break;
        case '$': {
            if (match('[')) {
                addToken(TokenType.DOLLAR_LEFT_BRACKET);
            } else {
                identifier();
            }
        }
            break;
//< two-char-tokens
      case '#':
          // A comment goes until the end of the line.
        while (peek() != '\n' && !isAtEnd()) advance();
        break;

      case ' ':
      case '\r':
      case '\t':
        // Ignore whitespace.
        break;

      case '\n':
        line++;
        addToken(TokenType.NEWLINE);
        break;
      default:

      if (isStringDelim(c)) {
        string(c);
      } else if (isDigit(c)) {
          number();
        } else if (isAlpha(c)) {
          identifier();
        } else {
          Simi.error(line, "Unexpected character.");
        }
        break;
    }
  }

  private void identifier() {
    while (isAlphaNumeric(peek())) advance();

    // See if the identifier is a reserved word.
    String text = source.substring(start, current);

    TokenType type = keywords.get(text);
    if (type == TokenType.NOT && matchPeek(TokenType.IN)) {
        type = TokenType.NOTIN;
    } else if (type == TokenType.IS && matchPeek(TokenType.NOT)) {
        type = TokenType.ISNOT;
    } else if (type == null) {
        type = TokenType.IDENTIFIER;
    }
    addToken(type);
  }

  private void number() {
    while (isDigit(peek())) advance();

    // Look for a fractional part.
    if (peek() == '.' && isDigit(peekNext())) {
      // Consume the "."
      advance();

      while (isDigit(peek())) advance();
    }

    addToken(TokenType.NUMBER,
        new SimiValue.Number(Double.parseDouble(source.substring(start, current))));
  }

  protected void string(char opener) {
    while (peek() != opener && !isAtEnd()) {
      if (peek() == '\n') line++;
      advance();
    }

    // Unterminated string.
    if (isAtEnd()) {
      Simi.error(line, "Unterminated string.");
      return;
    }

    // The closing ".
    advance();

    // Trim the surrounding quotes.
    String value = source.substring(start + 1, current - 1);
    addToken(TokenType.STRING, new SimiValue.String(value));
  }

    private String keywordString(TokenType type) {
      for (String s : keywords.keySet()) {
          if (keywords.get(s) == type) {
              return s;
          }
      }
      return null;
    }

    private boolean matchPeek(TokenType type) {
      String keyword = keywordString(type);
      int len = keyword.length();
      int end = current + len + 1;
      if (end < source.length() && source.substring(current + 1, end).equals(keyword)) {
          current = end;
          return true;
      }
      return false;
    }

  private boolean match(char expected) {
    if (isAtEnd()) return false;
    if (source.charAt(current) != expected) return false;

    current++;
    return true;
  }

  private char peek() {
    if (isAtEnd()) return '\0';
    return source.charAt(current);
  }
  private char peekNext() {
    if (current + 1 >= source.length()) return '\0';
    return source.charAt(current + 1);
  }

  private boolean isAlpha(char c) {
    return (c >= 'a' && c <= 'z') ||
           (c >= 'A' && c <= 'Z') ||
            c == '_' || c == '$';
  }

  private boolean isAlphaNumeric(char c) {
    return isAlpha(c) || isDigit(c);
  }

  private boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  } // [is-digit]

  private boolean isStringDelim(char c) {
    return c == '"' || c == '\'';
  }

  private boolean isAtEnd() {
    return current >= source.length();
  }

  private char advance() {
    current++;
    return source.charAt(current - 1);
  }

  private void addToken(TokenType type) {
    addToken(type, null);
  }

  private void addToken(TokenType type, SimiValue literal) {
    String text = source.substring(start, current);
    tokens.add(new Token(type, text, literal, line));
  }
}
