package net.globulus.simi;

import net.globulus.simi.api.SimiValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scanner {

  private static final Map<String, TokenType> keywords;

  static {
    keywords = new HashMap<>();
    keywords.put("and",    TokenType.AND);
    keywords.put("break",  TokenType.BREAK);
    keywords.put("class",  TokenType.CLASS);
    keywords.put("class_",  TokenType.CLASS_FINAL);
    keywords.put("class$",  TokenType.CLASS_OPEN);
    keywords.put("continue",    TokenType.CONTINUE);
    keywords.put("def",    TokenType.DEF);
    keywords.put("else",   TokenType.ELSE);
    keywords.put("elsif",  TokenType.ELSIF);
    keywords.put("false",  TokenType.FALSE);
    keywords.put("for",    TokenType.FOR);
    keywords.put("gu",     TokenType.GU);
    keywords.put("if",     TokenType.IF);
    keywords.put("import",     TokenType.IMPORT);
    keywords.put("in",     TokenType.IN);
    keywords.put("is",     TokenType.IS);
    keywords.put("ivic",     TokenType.IVIC);
    keywords.put("native",      TokenType.NATIVE);
    keywords.put("nil",    TokenType.NIL);
    keywords.put("not",    TokenType.NOT);
    keywords.put("or",     TokenType.OR);
    keywords.put("pass",    TokenType.PASS);
    keywords.put("print",          TokenType.PRINT);
    keywords.put("rescue",         TokenType.RESCUE);
    keywords.put("return",         TokenType.RETURN);
    keywords.put(Constants.SELF,   TokenType.SELF);
    keywords.put(Constants.SUPER,  TokenType.SUPER);
    keywords.put("true",   TokenType.TRUE);
    keywords.put("when",  TokenType.WHEN);
    keywords.put("while",  TokenType.WHILE);
    keywords.put("yield",  TokenType.YIELD);
  }

  private final String fileName;
  private final String source;
  private final Debugger debugger;
  private final List<Token> tokens = new ArrayList<>();

  private int start = 0;
  private int current = 0;
  private int line = 1;
  private int stringInterpolationParentheses = 0;
  private char lastStringOpener = '"';

  public Scanner(String fileName, String source, Debugger debugger) {
    this.fileName = fileName;
    this.source = source;
    this.debugger = debugger;
  }

  public List<Token> scanTokens(boolean addEof) {
    while (!isAtEnd()) {
      // We are at the beginning of the next lexeme.
      start = current;
      scanToken();
    }

    if (addEof) {
      tokens.add(new Token(TokenType.EOF, "", null, line, fileName));
    }
    return tokens;
  }

  private void scanToken() {
    char c = advance();
    switch (c) {
      case '(': {
        addToken(TokenType.LEFT_PAREN);
        if (stringInterpolationParentheses > 0) {
          stringInterpolationParentheses++;
        }
      } break;
      case ')': {
        addToken(TokenType.RIGHT_PAREN);
        if (stringInterpolationParentheses > 0) {
          stringInterpolationParentheses--;
          if (stringInterpolationParentheses == 0) {
            addToken(TokenType.PLUS);
            string(lastStringOpener);
          }
        }
      } break;
      case '[': addToken(TokenType.LEFT_BRACKET); break;
      case ']': addToken(TokenType.RIGHT_BRACKET); break;
      case '{': addToken(TokenType.LEFT_BRACE); break;
      case '}': addToken(TokenType.RIGHT_BRACE); break;
      case ',': addToken(TokenType.COMMA); break;
      case '.': {
        if (match('.')) {
          if (match('.')) {
            addToken(TokenType.DOT_DOT_DOT);
          } else {
            addToken(TokenType.DOT_DOT);
          }
        } else {
          addToken(TokenType.DOT);
        }
      } break;
      case ':': addToken(TokenType.COLON); break;
      case '@':
        addToken(TokenType.SELF);
        addToken(TokenType.DOT);
      break;
      case '?': {
        if (match('?')) {
          if (match('=')) {
            addToken(TokenType.QUESTION_QUESTION_EQUAL);
          } else {
            addToken(TokenType.QUESTION_QUESTION);
          }
        } else {
          addToken(TokenType.QUESTION);
        }
      } break;
      case '=': addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL); break;
      case '<': {
        if (match('>')) {
          addToken(TokenType.LESS_GREATER);
        } else if (match('=')) {
          addToken(TokenType.LESS_EQUAL);
        } else {
          addToken(TokenType.LESS);
        }
      } break;
      case '!': {
        if (match('!')) {
          addToken(TokenType.BANG_BANG);
        } else if (match('=')) {
          addToken(TokenType.BANG_EQUAL);
        } else {
          addToken(TokenType.BANG);
        }
      } break;
      case '>': addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER); break;
      case '+': addToken(match('=') ? TokenType.PLUS_EQUAL : TokenType.PLUS); break;
      case '-': addToken(match('=') ? TokenType.MINUS_EQUAL : TokenType.MINUS); break;
      case '/': {
        if (match('/')) {
          if ((match('='))) {
            addToken(TokenType.SLASH_SLASH_EQUAL);
          } else {
            addToken(TokenType.SLASH_SLASH);
          }
        } else if (match('=')) {
          addToken(TokenType.SLASH_EQUAL);
        } else {
          addToken(TokenType.SLASH);
        }
      } break;
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
          if (match('=')) {
            addToken(TokenType.DOLLAR_EQUAL);
          } else if (match('(')) {
            addToken(TokenType.DOLLAR_LEFT_PAREN);
          } else if (match('[')) {
              addToken(TokenType.DOLLAR_LEFT_BRACKET);
          } else {
              identifier();
          }
      } break;
      case '#': comment(); break;
      case '\\': {
        if (match('\n')) {
          line++;
        }
      } break;

      case ' ':
      case '\r':
      case '\t':
        // Ignore whitespace.
        break;

      case '\n': {
        line++;
        addToken(TokenType.NEWLINE);
      } break;
      case ';':
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
        error("Unexpected character.");
        }
        break;
    }
  }

  private void comment() {
    if (match('#')) { // Multi line
      while (!matchAll("##")) {
        if (peek() == '\n') {
          line++;
        }
        advance();
      }
    } else { // Single line
      // A comment goes until the end of the line.
      while (peek() != '\n' && !isAtEnd()) {
        advance();
      }
      if (debugger != null) {
        String comment = source.substring(start + 1, current);
        if (comment.trim().startsWith(Debugger.BREAKPOINT_LEXEME)) {
          int size = tokens.size();
          for (int i = size - 1; i >= 0; i--) {
            Token token = tokens.get(i);
            if (token.line == line) {
              token.hasBreakpoint = true;
            } else {
              break;
            }
          }
        }
      }
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
    } else if (type == TokenType.CLASS) {
      String candidateText = text + peek();
      TokenType candidateType = keywords.get(candidateText);
      if (candidateType != null) {
        type = candidateType;
        advance();
      }
    } else if (type == null) {
        type = TokenType.IDENTIFIER;
    }
    addToken(type);
  }

  private void number() {
    while (isDigitOrUnderscore(peek())) advance();
    // Look for a fractional part.
    if (peek() == '.' && isDigit(peekNext())) {
      // Consume the "."
      advance();

      while (isDigitOrUnderscore(peek())) advance();
    }
    // Exp notation
    if (peek() == 'e' || peek() == 'E') {
      if (isDigit(peekNext())) {
        advance();
      } else if (peekNext() == '+' || peekNext() == '-') {
        advance();
        advance();
      } else {
        error("Expected a digit or + or - after E!");
      }
      while (isDigitOrUnderscore(peek())) advance();
    }
    String numberString = source.substring(start, current).replace("_", "");
    SimiValue.Number literal;
    try {
      literal = new SimiValue.Number(Long.parseLong(numberString));
    } catch (NumberFormatException e) {
      literal = new SimiValue.Number(Double.parseDouble(numberString));
    }
    addToken(TokenType.NUMBER, literal);
  }

  protected void string(char opener) {
    while (peek() != opener && !isAtEnd()) {
      if (peek() == '\n') {
        line++;
      } else if (peek() == '\\') {
        char next = peekNext();
        if (next == opener) {
          advance();
        } else if (next == '(') { // String interpolation
          String valueSoFar = escapedString(start + 1, current);
          addToken(TokenType.STRING, new SimiValue.String(valueSoFar));
          addToken(TokenType.PLUS);
          advance(); // Skip the \
          advance(); // Skip the (
          addToken(TokenType.LEFT_PAREN);
          stringInterpolationParentheses = 1;
          return;
        }
      }
      advance();
    }

    // Unterminated string.
    if (isAtEnd()) {
      error("Unterminated string.");
      return;
    }

    // The closing ".
    advance();

    // Trim the surrounding quotes.
    String value = escapedString(start + 1, current - 1);
    addToken(TokenType.STRING, new SimiValue.String(value));
  }

  private String escapedString(int start, int stop) {
    String BACKSLASH_BACKSLASH_N_REPLACEMENT = "\\\\r";
    return source.substring(start, stop)
            .replace("\\\\n", BACKSLASH_BACKSLASH_N_REPLACEMENT)
            .replace("\\n", "\n")
            .replace(BACKSLASH_BACKSLASH_N_REPLACEMENT, "\\n")
            .replace("\\t", "\t")
            .replace("\\\"", "\"");
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
      if (keyword == null) {
        return false;
      }
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

  private boolean matchAll(String expected) {
    int end = current + expected.length();
    if (end >= source.length()) {
      return false;
    }
    if (!source.substring(current, end).equals(expected)) {
      return false;
    }
    current = end;
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
            c == '_';
  }

  private boolean isAlphaNumeric(char c) {
    return isAlpha(c) || isDigit(c);
  }

  private boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private boolean isDigitOrUnderscore(char c) {
    return isDigit(c) || c == '_';
  }

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
    tokens.add(new Token(type, text, literal, line, fileName));
  }
  
  private void error(String message) {
    ErrorHub.sharedInstance().error(Constants.EXCEPTION_SCANNER, fileName, line, message);
  }
}
