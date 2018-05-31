package net.globulus.simi;

import net.globulus.simi.api.Codifiable;
import net.globulus.simi.api.SimiBlock;
import net.globulus.simi.api.SimiStatement;
import net.globulus.simi.api.SimiValue;

import java.util.List;
import java.util.stream.Collectors;

abstract class Expr implements Codifiable {

  abstract <R> R accept(Visitor<R> visitor, Object... params);

  interface Visitor<R> {
    R visitAnnotationsExpr(Annotations expr);
    R visitAssignExpr(Assign expr);
    R visitBinaryExpr(Binary expr);
    R visitBlockExpr(Block expr, boolean newScope, boolean execute);
    R visitCallExpr(Call expr);
    R visitGetExpr(Get expr);
    R visitGroupingExpr(Grouping expr);
    R visitGuExpr(Gu expr);
    R visitIvicExpr(Ivic expr);
    R visitLiteralExpr(Literal expr);
    R visitLogicalExpr(Logical expr);
    R visitSetExpr(Set expr);
    R visitSuperExpr(Super expr);
    R visitSelfExpr(Self expr);
    R visitUnaryExpr(Unary expr);
    R visitVariableExpr(Variable expr);
    R visitObjectLiteralExpr(ObjectLiteral expr);
  }
    static class Block extends Expr implements SimiBlock {

      final Token declaration;
      final List<Token> params;
      final List<Stmt> statements;

        Block(Token declaration, List<Token> params, List<Stmt> statements) {
            this.declaration = declaration;
            this.params = params;
            this.statements = statements;
        }

        <R> R accept(Visitor<R> visitor, Object... params) {
            boolean newScope = (params.length < 1) ? true : (Boolean) params[0];
            boolean execute = (params.length < 2) ? true : (Boolean) params[1];
            return visitor.visitBlockExpr(this, newScope, execute);
        }

      boolean isNative() {
        return declaration.type == TokenType.NATIVE;
      }

      @Override
      public List<? extends SimiStatement> getStatements() {
        return statements;
      }

      @Override
      public void yield(int index) {
        throw new RuntimeException("Trying to yield a Expr.Block!");
      }

      public boolean isEmpty() {
        if (statements.size() != 1) {
          return false;
        }
        Stmt stmt = statements.get(0);
        if (!(stmt instanceof Stmt.Expression)) {
          return false;
        }
        Stmt.Expression expr = (Stmt.Expression) stmt;
        if (!(expr.expression instanceof Expr.Literal)) {
          return false;
        }
        return ((Expr.Literal) expr.expression).value instanceof Pass;
      }

      String toCode(String name) {
        String opener;
        if (declaration.type == TokenType.DEF || declaration.type == TokenType.NATIVE) {
          opener = declaration.type.toCode() + " ";
        } else {
          opener = "";
        }
        boolean needsParenthesis = !opener.isEmpty() || name != null;
        StringBuilder paramsBuilder = new StringBuilder();
        if (needsParenthesis) {
          paramsBuilder.append(TokenType.LEFT_PAREN.toCode());
        }
        paramsBuilder.append(params.stream()
                .map(p -> p.lexeme)
                .collect(Collectors.joining(TokenType.COMMA.toCode() + " "))
        );
        if (needsParenthesis) {
          paramsBuilder.append(TokenType.RIGHT_PAREN.toCode());
        }
        return new StringBuilder(opener)
                .append(paramsBuilder.toString())
                .append(TokenType.COLON.toCode())
                .append(TokenType.NEWLINE.toCode())
                .append(statements.stream()
                        .map(Codifiable::toCode)
                        .collect(Collectors.joining(TokenType.NEWLINE.toCode()))
                )
                .append(TokenType.END.toCode())
                .append(TokenType.NEWLINE.toCode())
                .toString();
      }

      @Override
      public String toCode() {
          return toCode(null);
      }
    }

    static class Annotations extends Expr {

      final List<Token> tokens;

      Annotations(List<Token> tokens) {
        this.tokens = tokens;
      }

      <R> R accept(Visitor<R> visitor, Object... params) {
        return visitor.visitAnnotationsExpr(this);
      }

      @Override
      public String toCode() {
        return tokens.stream()
                .map(t -> t.lexeme)
                .collect(Collectors.joining(TokenType.DOT.toCode()));
      }
    }

  static class Assign extends Expr {

    final Token name;
    final Expr value;
    final List<Stmt.Annotation> annotations;

    Assign(Token name, Expr value, List<Stmt.Annotation> annotations) {
      this.name = name;
      this.value = value;
      this.annotations = annotations;
    }

    <R> R accept(Visitor<R> visitor, Object... params) {
      return visitor.visitAssignExpr(this);
    }

    @Override
    public String toCode() {
      return new StringBuilder()
              .append(annotations != null
                      ? annotations.stream()
                          .map(Codifiable::toCode)
                          .collect(Collectors.joining(TokenType.NEWLINE.toCode()))
                      : ""
              )
              .append(name.lexeme)
              .append(" ").append(TokenType.EQUAL.toCode()).append(" ")
              .append(value.toCode())
              .toString();
    }
  }

  static class ObjectDecomp extends Expr {

    final List<Assign> assigns;

    ObjectDecomp(List<Assign> assigns) {
      this.assigns = assigns;
    }

    <R> R accept(Visitor<R> visitor, Object... params) {
      R value = null;
      for (Assign assign : assigns) {
        value = visitor.visitAssignExpr(assign);
      }
      return value;
    }

    @Override
    public String toCode() {
      return new StringBuilder(TokenType.LEFT_BRACKET.toCode())
              .append(assigns.stream()
                      .map(a -> a.name.lexeme)
                      .collect(Collectors.joining(TokenType.COMMA.toCode() + " "))
              )
              .append(TokenType.RIGHT_BRACKET.toCode())
              .append(" ").append(TokenType.EQUAL.toCode()).append(" ")
              .append(((Get) assigns.get(0).value).object.toCode())
              .toString();
    }
  }

  static class Binary extends Expr {

    final Expr left;
    final Token operator;
    final Expr right;

    Binary(Expr left, Token operator, Expr right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }

    <R> R accept(Visitor<R> visitor, Object... params) {
      return visitor.visitBinaryExpr(this);
    }

    @Override
    public String toCode() {
      return left.toCode() + " " + operator.type.toCode() + " " + right.toCode();
    }
  }

  static class Call extends Expr {

    final Expr callee;
    final Token paren;
    final List<Expr> arguments;

    Call(Expr callee, Token paren, List<Expr> arguments) {
      this.callee = callee;
      this.paren = paren;
      this.arguments = arguments;
    }

    <R> R accept(Visitor<R> visitor, Object... params) {
      return visitor.visitCallExpr(this);
    }

    @Override
    public String toCode() {
      return new StringBuilder(callee.toCode())
              .append(TokenType.LEFT_PAREN.toCode())
              .append(arguments.stream()
                      .map(Codifiable::toCode)
                      .collect(Collectors.joining(TokenType.COMMA.toCode() + " "))
              )
              .append(TokenType.RIGHT_PAREN.toCode())
              .toString();
    }
  }

  static class Get extends Expr {

    final Token origin;
    final Expr object;
    final Expr name;
    final Integer arity;

    Get(Token origin, Expr object, Expr name, Integer arity) {
      this.origin = origin;
      this.object = object;
      this.name = name;
      this.arity = arity;
    }

    <R> R accept(Visitor<R> visitor, Object... params) {
      return visitor.visitGetExpr(this);
    }

    @Override
    public String toCode() {
      return object.toCode() + TokenType.DOT.toCode() + name.toCode();
    }
  }

  static class Grouping extends Expr {

    final Expr expression;

    Grouping(Expr expression) {
      this.expression = expression;
    }

    <R> R accept(Visitor<R> visitor, Object... params) {
      return visitor.visitGroupingExpr(this);
    }

    @Override
    public String toCode() {
      return TokenType.LEFT_PAREN.toCode() + " " + expression.toCode() + " " + TokenType.RIGHT_PAREN.toCode();
    }
  }

  static class Gu extends Expr {

    final Expr expr;

    Gu(Expr expr) {
      this.expr = expr;
    }

    <R> R accept(Visitor<R> visitor, Object... params) {
      return visitor.visitGuExpr(this);
    }

    @Override
    public String toCode() {
      return TokenType.GU.toCode() + " " + expr.toCode();
    }
  }

  static class Ivic extends Expr {

    final Expr expr;

    Ivic(Expr expr) {
      this.expr = expr;
    }

    <R> R accept(Visitor<R> visitor, Object... params) {
      return visitor.visitIvicExpr(this);
    }

    @Override
    public String toCode() {
      return TokenType.IVIC.toCode() + " " + expr.toCode();
    }
  }

  static class Literal extends Expr {

    final SimiValue value;

    Literal(SimiValue value) {
      this.value = value;
    }

    <R> R accept(Visitor<R> visitor, Object... params) {
      return visitor.visitLiteralExpr(this);
    }

    @Override
    public String toCode() {
      return value.toCode();
    }
  }

  static class Logical extends Expr {

    final Expr left;
    final Token operator;
    final Expr right;

    Logical(Expr left, Token operator, Expr right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }

    <R> R accept(Visitor<R> visitor, Object... params) {
      return visitor.visitLogicalExpr(this);
    }

    @Override
    public String toCode() {
      return left.toCode() + " " + operator.type.toCode() + " " + right.toCode();
    }
  }

  static class Set extends Expr {

    final Token origin;
    final Expr object;
    final Expr name;
    final Expr value;

    Set(Token origin, Expr object, Expr name, Expr value) {
      this.origin = origin;
      this.object = object;
      this.name = name;
      this.value = value;
    }

    <R> R accept(Visitor<R> visitor, Object... params) {
      return visitor.visitSetExpr(this);
    }

    @Override
    public String toCode() {
      return new StringBuilder()
              .append(object.toCode())
              .append(TokenType.DOT.toCode())
              .append(name.toCode())
              .append(" ").append(TokenType.EQUAL.toCode()).append(" ")
              .append(value.toCode())
              .toString();
    }
  }

  static class Super extends Expr {

    final Token keyword;
    final Token superclass;
    final Token method;
    final Integer arity;

    Super(Token keyword, Token superclass, Token method, Integer arity) {
      this.keyword = keyword;
      this.superclass = superclass;
      this.method = method;
      this.arity = arity;
    }

    <R> R accept(Visitor<R> visitor, Object... params) {
      return visitor.visitSuperExpr(this);
    }

    @Override
    public String toCode() {
      StringBuilder sb = new StringBuilder(keyword.type.toCode());
      if (superclass != null) {
        sb.append(TokenType.LEFT_PAREN.toCode()).append(superclass.lexeme).append(TokenType.RIGHT_PAREN.toCode());
      }
      sb.append(TokenType.DOT.toCode()).append(method.lexeme);
      return sb.toString();
    }
  }

  static class Self extends Expr {

    final Token keyword;

    Self(Token keyword) {
      this.keyword = keyword;
    }

    <R> R accept(Visitor<R> visitor, Object... params) {
      return visitor.visitSelfExpr(this);
    }

    @Override
    public String toCode() {
      return keyword.type.toCode();
    }
  }

  static class Unary extends Expr {

    final Token operator;
    final Expr right;

    Unary(Token operator, Expr right) {
      this.operator = operator;
      this.right = right;
    }

    <R> R accept(Visitor<R> visitor, Object... params) {
      return visitor.visitUnaryExpr(this);
    }

    @Override
    public String toCode() {
      return operator.type.toCode() + " " + right.toCode();
    }
  }

    static class Variable extends Expr {

      final Token name;

        Variable(Token name) {
            this.name = name;
        }

        <R> R accept(Visitor<R> visitor, Object... params) {
            return visitor.visitVariableExpr(this);
        }

      @Override
      public String toCode() {
        return name.lexeme;
      }
    }

    static class ObjectLiteral extends Expr {

      final Token opener;
      final List<Expr> props;
      final boolean isDictionary;

        ObjectLiteral(Token opener, List<Expr> props, boolean isDictionary) {
            this.opener = opener;
            this.props = props;
            this.isDictionary = isDictionary;
        }

        <R> R accept(Visitor<R> visitor, Object... params) {
            return visitor.visitObjectLiteralExpr(this);
        }

      @Override
      public String toCode() {
        StringBuilder sb = new StringBuilder(opener.type.toCode()).append(TokenType.NEWLINE.toCode());
        for (Expr expr : props) {
          sb.append(expr.toCode()).append(TokenType.COMMA.toCode()).append(TokenType.NEWLINE.toCode());
        }
        sb.append(TokenType.RIGHT_BRACKET.toCode());
        return sb.toString();
      }
    }
}
