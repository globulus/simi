package net.globulus.simi;

import java.util.ArrayList;
//< Statements and State parser-imports
//> Control Flow import-arrays
//< Control Flow import-arrays
import java.util.List;
import java.util.stream.Collectors;

import static net.globulus.simi.TokenType.*;

class Parser {

  private static class ParseError extends RuntimeException {}

  private final List<Token> tokens;
  private int current = 0;

  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  List<Stmt> parse() {
    List<Stmt> statements = new ArrayList<>();
    while (!isAtEnd()) {
        if (match(IMPORT)) { // imports are handled during scanning phase
            advance(); // skip path string
            continue;
        }
        if (match(NEWLINE, PASS)) {
            continue;
        }
        statements.add(declaration());
    }
    return statements;
  }

  private Expr expression() {
    return assignment();
  }

  private Stmt declaration() {
    try {
      if (match(CLASS)) {
          return classDeclaration();
      }
      if (match(DEF, NATIVE)) {
          return function("function");
      }
      return statement();
    } catch (ParseError error) {
      synchronize();
      return null;
    }
  }

  private Stmt classDeclaration() {
    Token name = consume(IDENTIFIER, "Expect class name.");

    List<Expr> superclasses = null;
    if (check(LEFT_PAREN)) {
      superclasses = params("class", false)
              .stream()
              .map(Expr.Variable::new)
              .collect(Collectors.toList());
    }

    consume(COLON, "Expect ':' before class body.");

    List<Expr.Assign> constants = new ArrayList<>();
    List<Stmt.Function> methods = new ArrayList<>();
    while (!check(END) && !isAtEnd()) {
        if (match(DEF, NATIVE)) {
            methods.add(function("method"));
        } else {
            constants.add((Expr.Assign) assignment());
        }
    }

    consume(END, "Expect 'end' after class body.");

    return new Stmt.Class(name, superclasses, constants, methods);
  }

  private Stmt statement() {
    if (match(FOR)) {
        return forStatement();
    }
    if (match(IF)) {
        return ifStatement();
    }
    if (match(PRINT)) {
        return printStatement();
    }
    if (match(RETURN)) {
        return returnStatement();
    }
    if (match(WHILE)) {
        return whileStatement();
    }
//    if (match(COLON)) {
//        return block("block", true);
//    }
    return expressionStatement();
  }

  private Stmt forStatement() {
      Token var = consume(IDENTIFIER, "Expected identifier.");
      consume(IN, "Expected 'in'.");
      Stmt iterable = expressionStatement();
      Expr.Block body = block("for", true);
      return new Stmt.For(var, iterable, body);
  }

  private Stmt ifStatement() {
    Expr condition = expression();
    Expr.Block thenBranch = block("if", true);
    Expr.Block elseBranch = null;
    List<Stmt.Elsif> elsifs = new ArrayList<>();
    while (match(ELSIF)) {
        elsifs.add(new Stmt.Elsif(expression(), block("elsif", true)));
    }
    if (match(ELSE)) {
      elseBranch = block("else", true);
    }
    return new Stmt.If(new Stmt.Elsif(condition, thenBranch), elsifs, elseBranch);
  }

  private Stmt printStatement() {
    Expr value = expression();
    return new Stmt.Print(value);
  }

  private Stmt returnStatement() {
    Token keyword = previous();
    Expr value = null;
    if (!check(NEWLINE)) {
      value = expression();
    }
    consume(NEWLINE, "Expect newline after return value.");
    return new Stmt.Return(keyword, value);
  }

//  private Stmt varDeclaration() {
//    Token name = consume(IDENTIFIER, "Expect variable name.");
//
//    Expr initializer = null;
//    if (match(EQUAL)) {
//      initializer = expression();
//    }
//
//    consume(SEMICOLON, "Expect ';' after variable declaration.");
//    return new Stmt.Var(name, initializer);
//  }

  private Stmt whileStatement() {
    Expr condition = expression();
    Expr.Block block = block("while", false);
    return new Stmt.While(condition, block);
  }

  private Stmt expressionStatement() {
    Expr expr = expression();
    consume(NEWLINE, "Expect newline after expression.");
    return new Stmt.Expression(expr);
  }

  private Stmt.Function function(String kind) {
      Token declaration = previous();
    Token name = consume(IDENTIFIER, "Expect " + kind + " name.");
    Expr.Block block = block(kind, false);
    return new Stmt.Function(declaration, name, block);
  }

  private Expr.Block block(String kind, boolean lambda) {
    List<Token> params = params(kind, lambda);
    consume(COLON, "Expected a ':' at the start of block!");
    List<Stmt> statements = new ArrayList<>();
    if (match(NEWLINE)) {
        while (!check(END) && !isAtEnd()) {
            statements.add(declaration());
        }
        consume(END, "Expect 'end' after block.");
    } else {
        statements.add(statement());
    }
    return new Expr.Block(params, statements);
  }

  private List<Token> params(String kind, boolean lambda) {
    List<Token> params = new ArrayList<>();
    if (!check(LEFT_PAREN)) {
      if (lambda && !check(COLON)) {
        params.add(consume(IDENTIFIER, "Expect parameter name."));
      }
    } else {
      consume(LEFT_PAREN, "Expect '(' after " + kind + " name.");
      if (!check(RIGHT_PAREN)) {
        do {
          params.add(consume(IDENTIFIER, "Expect parameter name."));
        } while (match(COMMA));
      }
      consume(RIGHT_PAREN, "Expect ')' after parameters.");
    }
    return params;
  }

  private Expr assignment() {
    Expr expr = or();
    if (match(EQUAL)) {
      Token equals = previous();
      Expr value = assignment();

      if (expr instanceof Expr.Variable) {
        Token name = ((Expr.Variable)expr).name;
        return new Expr.Assign(name, value);
      } else if (expr instanceof Expr.Get) {
        Expr.Get get = (Expr.Get)expr;
        return new Expr.Set(get.object, get.name, value);
      }
      Simi.error(equals, "Invalid assignment target.");
    }
    return expr;
  }

  private Expr or() {
    Expr expr = and();
    while (match(OR)) {
      Token operator = previous();
      Expr right = and();
      expr = new Expr.Logical(expr, operator, right);
    }
    return expr;
  }

  private Expr and() {
    Expr expr = equality();
    while (match(AND)) {
      Token operator = previous();
      Expr right = equality();
      expr = new Expr.Logical(expr, operator, right);
    }
    return expr;
  }

  private Expr equality() {
    Expr expr = comparison();
    while (match(BANG_EQUAL, EQUAL_EQUAL, IS, ISNOT, IN, NOTIN)) {
      Token operator = previous();
      Expr right = comparison();
      expr = new Expr.Binary(expr, operator, right);
    }
    return expr;
  }

  private Expr comparison() {
    Expr expr = addition();
    while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      Token operator = previous();
      Expr right = addition();
      expr = new Expr.Binary(expr, operator, right);
    }
    return expr;
  }

  private Expr addition() {
    Expr expr = multiplication();
    while (match(MINUS, PLUS)) {
      Token operator = previous();
      Expr right = multiplication();
      expr = new Expr.Binary(expr, operator, right);
    }
    return expr;
  }

  private Expr multiplication() {
    Expr expr = unary();
    while (match(SLASH, STAR, MOD)) {
      Token operator = previous();
      Expr right = unary();
      expr = new Expr.Binary(expr, operator, right);
    }
    return expr;
  }

  private Expr unary() {
    if (match(NOT, MINUS)) {
      Token operator = previous();
      Expr right = unary();
      return new Expr.Unary(operator, right);
    }
    return call();
  }

  private Expr call() {
    Expr expr = primary();
    while (true) {
      if (match(LEFT_PAREN)) {
        expr = finishCall(expr);
      } else if (match(DOT)) {
          Token name;
          if (peek().type == NUMBER) {
              name = consume(NUMBER, "Expected a number or id after '.'.");
          } else {
              name = consume(IDENTIFIER, "Expected a number of id after '.'.");
          }
        expr = new Expr.Get(expr, name);
      } else {
        break;
      }
    }
    return expr;
  }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
//                if (arguments.size() >= 8) {
//                    Simi.error(peek(), "Cannot have more than 8 arguments.");
//                }
                arguments.add(expression());
            } while (match(COMMA));
        }
        Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments.");
        return new Expr.Call(callee, paren, arguments);
    }

  private Expr primary() {
    if (match(FALSE)) {
        return new Expr.Literal(false);
    }
    if (match(TRUE)) {
        return new Expr.Literal(true);
    }
    if (match(NIL)) {
        return new Expr.Literal(null);
    }

    if (match(NUMBER, STRING)) {
      return new Expr.Literal(previous().literal);
    }

    if (match(SUPER)) {
      Token keyword = previous();
      consume(DOT, "Expect '.' after 'super'.");
      Token method = consume(IDENTIFIER,
          "Expect superclass method name.");
      return new Expr.Super(keyword, method);
    }

    if (match(SELF)) {
        return new Expr.Self(previous());
    }

    if (match(LEFT_BRACKET, DOLLAR_LEFT_BRACKET)) {
        return objectLiteral();
    }

    if (match(DEF, NATIVE)) {
        return block("lambda", true);
    }

    if (match(IDENTIFIER)) {
      return new Expr.Variable(previous());
    }

    if (match(LEFT_PAREN)) {
      Expr expr = expression();
      consume(RIGHT_PAREN, "Expect ')' after expression.");
      return new Expr.Grouping(expr);
    }

    throw error(peek(), "Expect expression.");
  }

  private Expr objectLiteral() {
      Token opener = previous();
      List<Expr> props = new ArrayList<>();
      if (!check(RIGHT_BRACKET)) {
          do {
              props.add(expression());
          } while (match(COMMA));
      }
      consume(RIGHT_BRACKET, "Expect ']' at the end of object.");
      return new Expr.ObjectLiteral(opener, props);
  }

    private boolean matchAll(TokenType... types) {
      for (int i = 0; i < types.length; i++) {
          int index = current + i;
          if (index >= tokens.size()) {
              return false;
          }
          if (tokens.get(index).type != types[i]) {
              return false;
          }
      }
      for (int i = 0; i < types.length; i++) {
          advance();
      }
      return true;
    }

  private boolean match(TokenType... types) {
    for (TokenType type : types) {
      if (check(type)) {
        advance();
        return true;
      }
    }
    return false;
  }

  private Token consume(TokenType type, String message) {
    if (check(type)) return advance();

    throw error(peek(), message);
  }

  private boolean check(TokenType tokenType) {
    if (isAtEnd()) return false;
    return peek().type == tokenType;
  }

  private Token advance() {
    if (!isAtEnd()) current++;
    return previous();
  }

  private boolean isAtEnd() {
    return peek().type == EOF;
  }

  private Token peek() {
    return tokens.get(current);
  }

  private Token previous() {
    return tokens.get(current - 1);
  }

  private ParseError error(Token token, String message) {
    Simi.error(token, message);
    return new ParseError();
  }

  private void synchronize() {
    advance();

    while (!isAtEnd()) {
//      if (previous().type == SEMICOLON) return;

      switch (peek().type) {
        case CLASS:
            case DEF:
        case FOR:
        case IF:
        case WHILE:
        case PRINT:
        case RETURN:
          return;
      }

      advance();
    }
  }
}
