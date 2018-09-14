package net.globulus.simi;

import net.globulus.simi.api.SimiValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static net.globulus.simi.TokenType.*;

class Parser {

  private static final String LAMBDA = "lambda";
  private static final String FUNCTION = "function";
  private static final String METHOD = "method";

  private static class ParseError extends RuntimeException {}

  private final List<Token> tokens;
  private int current = 0;

  private final Debugger debugger;

  private List<Stmt.Annotation> annotations = new ArrayList<>();

  Parser(List<Token> tokens, Debugger debugger) {
    this.tokens = tokens;
    this.debugger = debugger;
  }

  List<Stmt> parse() {
    List<Stmt> statements = new ArrayList<>();
    while (!isAtEnd()) {
        if (match(IMPORT)) {
            Token keyword = previous();
            if (match(IDENTIFIER)) {
              statements.add(new Stmt.Import(keyword, new Expr.Variable(previous())));
            } else { // String imports are handled during scanning phase
              continue;
            }
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
      if (match(CLASS, CLASS_FINAL, CLASS_OPEN)) {
          return classDeclaration();
      }
      if (match(DEF)) {
          return function(FUNCTION);
      }
      if (match(BANG)) {
        return annotation();
      }
      if (match(IMPORT)) {
        Token keyword = previous();
        if (match(IDENTIFIER)) {
          return new Stmt.Import(keyword, new Expr.Variable(previous()));
        }
      }
      return statement(false);
    } catch (ParseError error) {
      synchronize();
      return null;
    }
  }

  private Stmt.Class classDeclaration() {
    Token opener = previous();
    Token name = consume(IDENTIFIER, "Expect class name.");

    List<Expr> superclasses = null;
    if (match(LEFT_PAREN)) {
      if (!check(RIGHT_PAREN)) {
        superclasses = new ArrayList<>();
        do {
          superclasses.add(call());
        } while (match(COMMA));
      }
      consume(RIGHT_PAREN, "Expect ')' after superclasses.");
    }

    consume(COLON, "Expect ':' before class body.");

    List<Expr.Assign> constants = new ArrayList<>();
    List<Expr> mixins = new ArrayList<>();
    List<Stmt.Class> innerClasses = new ArrayList<>();
    List<Stmt.Function> methods = new ArrayList<>();
    while (!check(END) && !isAtEnd()) {
        if (match(NEWLINE)) {
          continue;
        }
        if (match(DEF)) {
            methods.add(function(METHOD));
        } else if (match(CLASS, CLASS_FINAL, CLASS_OPEN)) {
            innerClasses.add(classDeclaration());
        } else if (match(IMPORT)) {
          Expr expr = call();
          if (expr instanceof Expr.Get || expr instanceof Expr.Variable) {
            mixins.add(expr);
          } else {
            error(previous(), "Expected a get or variable expr after mixin import.");
          }
        } else if (match(BANG)) {
            annotations.add(annotation());
        } else {
            Expr expr = assignment();
            if (expr instanceof Expr.Assign) {
              constants.add((Expr.Assign) expr);
            }
        }
    }

    consume(END, "Expect 'end' after class body.");

    return new Stmt.Class(opener, name, superclasses, mixins, constants, innerClasses, methods, getAnnotations());
  }

  private Stmt.Annotation annotation() {
    Expr expr = null;
    if (peek().type == LEFT_BRACKET) {
      advance();
      expr = objectLiteral();
    } else if (peek().type == IDENTIFIER) {
      expr = call();
    } else {
      ErrorHub.sharedInstance().error(peek(), "Annotation expect either an object literal or a constructor invocation!");
    }
    checkStatementEnd(false);
    return new Stmt.Annotation(expr);
  }

  private Stmt statement(boolean lambda) {
    if (match(FOR)) {
        return forStatement();
    }
    if (match(IF)) {
        return ifStatement();
    }
    if (match(WHEN)) {
      return whenStatement();
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
    if (match(YIELD)) {
      return yieldStatement(lambda);
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
    while (match(ELSIF, NEWLINE)) {
        if (previous().type == ELSIF) {
          elsifs.add(new Stmt.Elsif(expression(), block("elsif", true)));
        }
    }
    if (match(ELSE)) {
      elseBranch = block("else", true);
    }
    return new Stmt.If(new Stmt.Elsif(condition, thenBranch), elsifs, elseBranch);
  }

  private Stmt whenStatement() {
    Token when = previous();
    Expr left = call();
    consume(COLON, "Expect a ':' after when.");
    consume(NEWLINE, "Expect a newline after when ':.");
    Stmt.Elsif firstElsif = null;
    List<Stmt.Elsif> elsifs = new ArrayList<>();
    Expr.Block elseBranch = null;
    while (!check(END) && !isAtEnd()) {
      if (match(NEWLINE)) {
        continue;
      }
      List<Expr.Binary> conditions = new ArrayList<>();
      do {
        Token op;
        if (match(IS, ISNOT, IN, NOTIN)) {
          op = previous();
        } else if (match(ELSE)) {
          elseBranch = block("else", true);
          break;
        } else {
          op = new Token(EQUAL_EQUAL, null, null, when.line, when.file);
        }
        Expr right = call();
        conditions.add(new Expr.Binary(left, op, right));
        match(OR);
      } while (!check(COLON));
      if (conditions.isEmpty()) {
        continue;
      }
      Expr condition = conditions.get(0);
      Token or = new Token(OR, null, null, when.line, when.file);
      for (int i = 1; i < conditions.size(); i++) {
        condition = new Expr.Logical(condition, or, conditions.get(i));
      }
      Stmt.Elsif elsif = new Stmt.Elsif(condition, block("when", true));
      if (firstElsif == null) {
        firstElsif = elsif;
      } else {
        elsifs.add(elsif);
      }
    }
    consume(END, "Expect end at the end of when.");
    return new Stmt.If(firstElsif, elsifs, elseBranch);
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

  private Stmt yieldStatement(boolean lambda) {
    Token keyword = previous();
    Expr value = null;
    if (!check(NEWLINE)) {
      value = expression();
    }
    checkStatementEnd(lambda);
    return new Stmt.Yield(keyword, value);
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
      ErrorHub.sharedInstance().error(keyword, "Rescue block expects exactly 1 parameter!");
    }
    return new Stmt.Rescue(keyword, block);
  }

  private Stmt.Expression expressionStatement(boolean lambda) {
    Expr expr = expression();
    if (!(expr instanceof Expr.Assign) || !(((Expr.Assign) expr).value instanceof Expr.Block)) {
      checkStatementEnd(lambda);
    }
    return new Stmt.Expression(expr);
  }

  private void checkStatementEnd(boolean lambda) {
    if (match(NEWLINE, EOF)) {
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
    List<Stmt.Annotation> annotations = getAnnotations();
    String blockKind = name.lexeme.equals(Constants.INIT) ? Constants.INIT : kind;
    Expr.Block block = block(declaration, blockKind, false, false);
    List<Stmt> statements = null;
    // Check empty init or set* and put assignments into it
    if (block.isEmpty()) {
      if (name.lexeme.equals(Constants.INIT)) {
        statements = new ArrayList<>();
        for (Expr param : block.params) {
          Expr.Variable paramName = extractParamName(param);
          statements.add(new Stmt.Expression(new Expr.Set(name,
                  new Expr.Self(Token.self(), null), paramName, paramName)));
        }
      } else if (name.lexeme.startsWith(Constants.SET) && block.params.size() == 1) {
          statements = new ArrayList<>();
          int offset = Constants.SET.length();
        Token valueName = new Token(TokenType.IDENTIFIER, name.lexeme.substring(offset, offset + 1).toLowerCase()
                + name.lexeme.substring(offset + 1), null, name.line, name.file);
        Expr.Variable paramName = extractParamName(block.params.get(0));
        statements.add(new Stmt.Expression(new Expr.Set(name,
                new Expr.Self(Token.self(), null), new Expr.Variable(valueName), paramName)));
      }
    }
    if (statements == null) {
        statements = block.statements;
    }
    addParamChecks(declaration, block.params, statements);
    block = new Expr.Block(declaration, block.params, statements, true);
    return new Stmt.Function(name, block, annotations);
  }

  private Expr.Block block(String kind, boolean lambda) {
    return block(null, kind, lambda, true);
  }

  private Expr.Block block(Token declaration, String kind, boolean lambda, boolean addParamChecks) {
    if (declaration == null) {
      declaration = previous();
    }
    List<Expr> params = params(kind, lambda);
    consume(COLON, "Expected a ':' at the start of block!");
    List<Stmt> statements = getBlockStatements(declaration, kind);
    if (addParamChecks) {
      addParamChecks(declaration, params, statements);
    }
    return new Expr.Block(declaration, params, statements,
            kind.equals(LAMBDA) || kind.equals(FUNCTION) || kind.equals(METHOD));
  }

  private List<Stmt> getBlockStatements(Token declaration, String kind) {
    List<Stmt> statements = new ArrayList<>();
    if (match(NEWLINE)) {
      while (!check(END) && !isAtEnd()) {
        if (match(NEWLINE, PASS)) {
          continue;
        }
        statements.add(declaration());
      }
      consume(END, "Expect 'end' after block.");
    } else {
      Stmt stmt = statement(true);
      if (kind.equals(LAMBDA) && stmt instanceof Stmt.Expression) {
        stmt = new Stmt.Return(new Token(RETURN, null, null, declaration.line, declaration.file), ((Stmt.Expression) stmt).expression);
      }
      statements.add(stmt);
    }
    return statements;
  }

  private List<Expr> params(String kind, boolean lambda) {
    List<Expr> params = new ArrayList<>();
    if (!check(LEFT_PAREN)) {
      if (lambda && !check(COLON)) {
        Token id = consume(IDENTIFIER, "Expect parameter name.");
        params.add(new Expr.Variable(id));
      }
    } else {
      consume(LEFT_PAREN, "Expect '(' after " + kind + " name.");
      if (!check(RIGHT_PAREN)) {
        do {
          Expr.Variable id = new Expr.Variable(consume(IDENTIFIER, "Expect parameter name."));
          if (match(IS)) {
            Token is = previous();
            Expr type = call();
            params.add(new Expr.Binary(id, is, type));
          } else {
            params.add(id);
          }
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
      if (type == LEFT_PAREN || type == LEFT_BRACKET || type == DOLLAR_LEFT_BRACKET) {
        parenCount++;
      } else if (type == RIGHT_PAREN || type == RIGHT_BRACKET) {
        if (parenCount == 0) {
          break;
        } else {
          parenCount--;
        }
      } else if (type == COMMA && parenCount == 0) {
        count++;
      }
    }
    return count;
  }

  private Expr assignment() {
    Expr expr = or();
    if (match(EQUAL, DOLLAR_EQUAL, PLUS_EQUAL, MINUS_EQUAL, STAR_EQUAL, SLASH_EQUAL,
            SLASH_SLASH_EQUAL, MOD_EQUAL, QUESTION_QUESTION_EQUAL)) {
      Token equals = previous();
      if (match(YIELD)) {
        Token keyword = previous();
        Expr call = call();
        if (call instanceof Expr.Call) {
          return new Expr.Yield(expr, equals, keyword, (Expr.Call) call);
        } else {
          ErrorHub.sharedInstance().error(keyword, "yield expressions must involve a call!");
        }
      }
      Expr value = assignment();
      return getAssignExpr(this, expr, equals, value);
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
    while (match(SLASH, SLASH_SLASH, STAR, MOD)) {
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
    if (match(GU)) {
      return new Expr.Gu(unary());
    }
    if (match(IVIC)) {
      return new Expr.Ivic(unary());
    }
    if (match(BANG_BANG)) {
      List<Token> tokens = new ArrayList<>();
      while (match(IDENTIFIER)) {
        tokens.add(previous());
        match(DOT);
      }
      if (tokens.isEmpty()) {
        ErrorHub.sharedInstance().error(peek(), "Annotations operator needs params!");
      }
      return new Expr.Annotations(tokens);
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
            name = primary();
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

    if (match(NATIVE)) {
      return new Expr.Literal(new Native());
    }

    if (match(NUMBER, STRING)) {
      return new Expr.Literal(previous().literal);
    }

    if (match(SUPER)) {
      Token keyword = previous();
      Token superclass;
      if (match(LEFT_PAREN)) {
        superclass = consume(IDENTIFIER, "Expected superclass name in parentheses!");
        consume(RIGHT_PAREN, "Expected ')' after superclass specification!");
      } else {
        superclass = null;
      }
      consume(DOT, "Expect '.' after 'super'.");
      Token method = consume(IDENTIFIER,
          "Expect superclass method name.");
      Integer arity = peekParams();
      return new Expr.Super(keyword, superclass, method, arity);
    }

    if (match(SELF)) {
      Token previous = previous();
      Token specifier = null;
       if (peekSequence(LEFT_PAREN, DEF, RIGHT_PAREN)) {
         specifier = new Token(DEF, Constants.SELF_DEF, null, previous.line, previous.file);
         advance();
         advance();
         advance();
       }
        return new Expr.Self(new Token(TokenType.SELF, Constants.SELF, null, previous.line, previous.file), specifier);
    }

    if (match(LEFT_BRACKET, DOLLAR_LEFT_BRACKET)) {
        return objectLiteral();
    }

    if (match(DEF)) {
        return block(LAMBDA, true);
    }

    if (match(COLON)) {
      Token declaration = new Token(DEF, null, null, previous().line, previous().file);
      return new Expr.Block(declaration, new ArrayList<>(), getBlockStatements(declaration, LAMBDA), true);
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
        dictionary = peekSequence(IDENTIFIER, EQUAL) || peekSequence(STRING, EQUAL);
        do {
          matchAllNewlines();
          props.add(dictionary ? assignment() : or());
          matchAllNewlines();
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
    if (isAtEnd()) return tokenType == EOF;
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

  private static Token operatorFromAssign(Token assignOp) {
    TokenType type;
    switch (assignOp.type) {
      case PLUS_EQUAL:
        type = PLUS;
        break;
      case MINUS_EQUAL:
        type = MINUS;
        break;
      case STAR_EQUAL:
        type = STAR;
        break;
      case SLASH_EQUAL:
        type = SLASH;
        break;
      case SLASH_SLASH_EQUAL:
        type = SLASH_SLASH;
        break;
      case MOD_EQUAL:
        type = MOD;
        break;
      case QUESTION_QUESTION_EQUAL:
        type = QUESTION_QUESTION;
        break;
      default:
        throw new IllegalArgumentException("Unable to process assignment operator: " + assignOp.type);
    }
    return new Token(type, assignOp.lexeme, null, assignOp.line, assignOp.file);
  }

  private ParseError error(Token token, String message) {
    ErrorHub.sharedInstance().error(token, message);
    return new ParseError();
  }

  private void synchronize() {
    advance();

    while (!isAtEnd()) {
//      if (previous().type == SEMICOLON) return;

      switch (peek().type) {
        case CLASS:
        case CLASS_FINAL:
        case CLASS_OPEN:
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

  private List<Stmt.Annotation> getAnnotations() {
      if (annotations.isEmpty()) {
          return null;
      }
      List<Stmt.Annotation> copy = Collections.unmodifiableList(new ArrayList<>(annotations));
      annotations.clear();
      return copy;
  }

  private void addParamChecks(Token declaration, List<Expr> params, List<Stmt> stmts) {
    for (Expr param : params) {
      if (param instanceof Expr.Binary) {
        Expr.Binary typeCheck = (Expr.Binary) param;
        Expr paramName = typeCheck.left;
        Expr paramType = typeCheck.right;
          List<Stmt> exceptionStmt = Collections.singletonList(new Stmt.Expression(
                  new Expr.Call(new Expr.Get(declaration,
                          new Expr.Call(new Expr.Variable(Token.named(Constants.EXCEPTION_TYPE_MISMATCH)), declaration,
                                  Arrays.asList(paramName, typeCheck.right)), new Expr.Variable(Token.named(Constants.RAISE)), 0),
                  declaration, Collections.emptyList())
          ));
          stmts.add(0, new Stmt.If(
                  new Stmt.Elsif(new Expr.Binary(paramName, new Token(ISNOT, null, null, declaration.line, declaration.file), paramType),
                          new Expr.Block(typeCheck.operator, Collections.emptyList(),
                                  exceptionStmt, true)), Collections.emptyList(), null));
      }
    }
  }

  static Expr getAssignExpr(Parser parser, Expr expr, Token equals, Expr value) {
    if (expr instanceof Expr.Literal && ((Expr.Literal) expr).value instanceof SimiValue.String) {
      Token literal = new Token(TokenType.STRING, null, ((Expr.Literal) expr).value, equals.line, equals.file);
      return new Expr.Assign(literal, equals, value, (parser != null) ? parser.getAnnotations() : null);
    } else if (expr instanceof Expr.Variable) {
      Token name = ((Expr.Variable)expr).name;
      if (equals.type == EQUAL || equals.type == DOLLAR_EQUAL) {
        return new Expr.Assign(name, equals, value, (parser != null) ? parser.getAnnotations() : null);
      } else {
        return new Expr.Assign(name, new Token(DOLLAR_EQUAL, null, null, equals.line, equals.file),
                new Expr.Binary(expr, operatorFromAssign(equals), value),
                (parser != null) ? parser.getAnnotations() : null);
      }
    } else if (expr instanceof Expr.Get) { // Setter
      if (equals.type == EQUAL) {
        Expr.Get get = (Expr.Get) expr;
        return new Expr.Set(get.origin, get.object, get.name, value);
      } else {
        ErrorHub.sharedInstance().error(equals, "Cannot use compound assignment operators with setters!");
      }
    } else if (expr instanceof Expr.ObjectLiteral) { // Object decomposition
      Expr.ObjectLiteral objectLiteral = (Expr.ObjectLiteral) expr;
      if (objectLiteral.isDictionary || objectLiteral.opener.type == DOLLAR_LEFT_BRACKET) {
        ErrorHub.sharedInstance().error(equals.line, "Invalid object decomposition syntax.");
      }
      List<Expr.Assign> assigns = new ArrayList<>();
      List<Stmt.Annotation> annotations = (parser != null) ? parser.getAnnotations() : null;
      for (int i = 0; i < objectLiteral.props.size(); i++) {
        Expr prop = objectLiteral.props.get(i);
        Token name = ((Expr.Variable) prop).name;
        Expr getByName = new Expr.Get(name, value, prop, null);
        Expr getByIndex = new Expr.Get(name, value, new Expr.Literal(new SimiValue.Number(i)), null);
        Expr nilCoalescence = new Expr.Binary(getByName, new Token(TokenType.QUESTION_QUESTION, null, null, name.line, name.file), getByIndex);
        assigns.add(new Expr.Assign(name, equals, nilCoalescence, annotations));
      }
      return new Expr.ObjectDecomp(assigns);
    }
    ErrorHub.sharedInstance().error(equals, "Invalid assignment target.");
    return null;
  }

  private Expr.Variable extractParamName(Expr param) {
    if (param instanceof Expr.Variable) {
      return (Expr.Variable) param;
    }
    Expr.Binary typeCheck = (Expr.Binary) param;
    return (Expr.Variable) typeCheck.left;
  }
}
