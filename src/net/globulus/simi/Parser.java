package net.globulus.simi;

import net.globulus.simi.api.SimiValue;

import java.util.ArrayList;
//< Statements and State parser-imports
//> Control Flow import-arrays
//< Control Flow import-arrays
import java.util.List;
import java.util.stream.Collectors;

import static net.globulus.simi.TokenType.*;

class Parser {

  private static final String LAMBDA = "lambda";

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
        if (match(NEWLINE, PASS, GU)) {
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
      return statement(false);
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
        if (match(NEWLINE)) {
          continue;
        }
        if (match(DEF, NATIVE)) {
            methods.add(function("method"));
        } else {
            constants.add((Expr.Assign) assignment());
        }
    }

    consume(END, "Expect 'end' after class body.");

    return new Stmt.Class(name, superclasses, constants, methods);
  }

  private Stmt statement(boolean lambda) {
    if (match(FOR)) {
        return forStatement();
    }
    if (match(IF)) {
        return ifStatement();
    }
    if (match(PRINT)) {
        return printStatement(lambda);
    }
    if (match(RETURN)) {
        return returnStatement(lambda);
    }
    if (match(WHILE)) {
        return whileStatement();
    }
    if (match(BREAK)) {
      return breakStatement();
    }
    if (match(CONTINUE)) {
      return continueStatement();
    }
    if (match(RESCUE)) {
      return rescueStatement();
    }
//    if (match(COLON)) {
//        return block("block", true);
//    }
    return expressionStatement(lambda);
  }

  private Stmt forStatement() {
      Token var = consume(IDENTIFIER, "Expected identifier.");
      consume(IN, "Expected 'in'.");
      Expr iterable = expression();
      Expr.Block body = block("for", true);
      return new Stmt.For(new Expr.Variable(var), iterable, body);
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

  private Stmt printStatement(boolean lambda) {
    Expr value = expressionStatement(lambda).expression;
    return new Stmt.Print(value);
  }

  private Stmt returnStatement(boolean lambda) {
    Token keyword = previous();
    Expr value = null;
    if (!check(NEWLINE)) {
      value = expression();
    }
    checkStatementEnd(lambda);
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
    Expr.Block block = block("while", true);
    return new Stmt.While(condition, block);
  }

  private Stmt breakStatement() {
    Token name = previous();
    return new Stmt.Break(name);
  }

  private Stmt continueStatement() {
    Token name = previous();
    return new Stmt.Continue(name);
  }

  private Stmt rescueStatement() {
    Token keyword = previous();
    Expr.Block block = block("rescue", true);
    if (block.params.size() != 1) {
      Simi.error(keyword, "Rescue block expects exactly 1 parameter!");
    }
    return new Stmt.Rescue(keyword, block);
  }

  private Stmt.Expression expressionStatement(boolean lambda) {
    Expr expr = expression();
    checkStatementEnd(lambda);
    return new Stmt.Expression(expr);
  }

  private void checkStatementEnd(boolean lambda) {
    if (match(NEWLINE)) {
      return;
    }
    if (lambda) {
      Token token = peek();
      if (token.type == COMMA || token.type == RIGHT_PAREN || token.type == RIGHT_BRACKET) {
        return;
      }
    }
    error(peek(), "Unterminated lambda expression!");
  }

  private Stmt.Function function(String kind) {
      Token declaration = previous();
    Token name = consume(IDENTIFIER, "Expect " + kind + " name.");
    String blockKind = name.lexeme.equals(Constants.INIT) ? Constants.INIT : kind;
    Expr.Block block = block(declaration, blockKind, false);

    // Check empty init and put assignments into it
    if (name.lexeme.equals(Constants.INIT) && block.isEmpty()) {
      List<Stmt> statements = new ArrayList<>();
      for (Token param : block.params) {
        statements.add(new Stmt.Expression(new Expr.Set(name,
                new Expr.Self(Token.self()), new Expr.Variable(param), new Expr.Variable(param))));
      }
      block = new Expr.Block(declaration, block.params, statements);
    }

    return new Stmt.Function(name, block);
  }

  private Expr.Block block(String kind, boolean lambda) {
    return block(null, kind, lambda);
  }

  private Expr.Block block(Token declaration, String kind, boolean lambda) {
    if (declaration == null) {
      declaration = previous();
    }
    List<Token> params = params(kind, lambda);
    consume(COLON, "Expected a ':' at the start of block!");
    List<Stmt> statements = new ArrayList<>();
    if (match(NEWLINE)) {
        while (!check(END) && !isAtEnd()) {
            if (match(NEWLINE, PASS, GU)) {
              continue;
            }
            statements.add(declaration());
        }
        consume(END, "Expect 'end' after block.");
    } else {
        Stmt stmt = statement(true);
        if (kind.equals(LAMBDA) && stmt instanceof Stmt.Expression) {
          stmt = new Stmt.Return(declaration, ((Stmt.Expression) stmt).expression);
        }
        statements.add(stmt);
    }
    return new Expr.Block(declaration, params, statements);
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

  private Integer peekParams() {
    if (!check(LEFT_PAREN)) {
      return null;
    }
    if (peekSequence(LEFT_PAREN, RIGHT_PAREN)) {
      return 0;
    }
    int len = tokens.size();
    int count = 1;
    int parenCount = 0;
    for (int i = current + 1; i < len; i++) {
      TokenType type = tokens.get(i).type;
      if (type == LEFT_PAREN) {
        parenCount++;
      } else if (type == RIGHT_PAREN) {
        if (parenCount == 0) {
          break;
        } else {
          parenCount--;
        }
      } else if (type == COMMA) {
        count++;
      }
    }
    return count;
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
        return new Expr.Set(get.origin, get.object, get.name, value);
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
    while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL, LESS_GREATER)) {
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
    Expr expr = nilCoalescence();
    while (match(SLASH, STAR, MOD)) {
      Token operator = previous();
      Expr right = nilCoalescence();
      expr = new Expr.Binary(expr, operator, right);
    }
    return expr;
  }

  private Expr nilCoalescence() {
    Expr expr = unary();
    while (match(QUESTION_QUESTION)) {
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
          Token dot = previous();
          Expr name;
          if (peek().type == NUMBER) {
              name = new Expr.Variable(consume(NUMBER, "Expected a number or id after '.'."));
          } else if (peek().type == LEFT_PAREN) {
            name = or();
          } else {
            name = new Expr.Variable(consume(IDENTIFIER, "Expected a number of id after '.'."));
          }
          Integer arity = peekParams();
        expr = new Expr.Get(dot, expr, name, arity);
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
                matchSequence(IDENTIFIER, EQUAL); // allows for named params, e.g substr(start=1,end=2)
                arguments.add(expression());
            } while (match(COMMA));
        }
        Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments.");
        return new Expr.Call(callee, paren, arguments);
    }

  private Expr primary() {
    if (match(FALSE)) {
        return new Expr.Literal(new SimiValue.Number(false));
    }
    if (match(TRUE)) {
        return new Expr.Literal(new SimiValue.Number(true));
    }
    if (match(NIL)) {
        return new Expr.Literal(null);
    }

    if (match(PASS)) {
      return new Expr.Literal(new Pass());
    }

    if (match(NUMBER, STRING)) {
      return new Expr.Literal(previous().literal);
    }

    if (match(SUPER)) {
      Token keyword = previous();
      consume(DOT, "Expect '.' after 'super'.");
      Token method = consume(IDENTIFIER,
          "Expect superclass method name.");
      Integer arity = peekParams();
      return new Expr.Super(keyword, method, arity);
    }

    if (match(SELF)) {
      Token previous = previous();
        return new Expr.Self(new Token(TokenType.SELF, Constants.SELF, null, previous.line));
    }

    if (match(LEFT_BRACKET, DOLLAR_LEFT_BRACKET)) {
        return objectLiteral();
    }

    if (match(DEF, NATIVE)) {
        return block(LAMBDA, true);
    }

    if (match(IDENTIFIER)) {
      return new Expr.Variable(previous());
    }

    if (match(LEFT_PAREN)) {
      Expr expr = expression();
      consume(RIGHT_PAREN, "Expect ')' after expression.");
      return new Expr.Grouping(expr);
    }

    if (match(QUESTION)) {
      return new Expr.Unary(previous(), primary());
    }

    throw error(peek(), "Expect expression.");
  }

  private Expr objectLiteral() {
      Token opener = previous();
      List<Expr> props = new ArrayList<>();
      boolean dictionary = true;
      if (!check(RIGHT_BRACKET)) {
        matchAllNewlines();
        dictionary = peekSequence(IDENTIFIER, EQUAL);
        do {
          matchAllNewlines();
          props.add(dictionary ? assignment() : or());
        } while (match(COMMA));
        matchAllNewlines();
      }
      consume(RIGHT_BRACKET, "Expect ']' at the end of object.");
      return new Expr.ObjectLiteral(opener, props, dictionary);
  }

  private void matchAllNewlines() {
    while (match(NEWLINE)) { }
  }

    private boolean matchSequence(TokenType... types) {
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

  private boolean peekSequence(TokenType... tokenTypes) {
    if (current + tokenTypes.length >= tokens.size()) {
      return false;
    }
    for (int i = 0; i < tokenTypes.length; i++) {
      if (tokens.get(current + i).type != tokenTypes[i]) {
        return false;
      }
    }
    return true;
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
