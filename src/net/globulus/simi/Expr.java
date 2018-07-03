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
      final boolean canReturn;

        Block(Token declaration, List<Token> params, List<Stmt> statements, boolean canReturn) {
            this.declaration = declaration;
            this.params = params;
            this.statements = statements;
            this.canReturn = canReturn;
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

      @Override
      public boolean canReturn() {
        return canReturn;
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

      String toCode(int indentationLevel, boolean ignoreFirst, String name) {
        String opener;
        if (declaration.type == TokenType.DEF || declaration.type == TokenType.NATIVE) {
          opener = declaration.type.toCode() + " ";
        } else {
          opener = "";
        }
        if (name != null) {
          opener += name;
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
        return new StringBuilder(ignoreFirst ? "" : Codifiable.getIndentation(indentationLevel))
                .append(opener)
                .append(paramsBuilder.toString())
                .append(TokenType.COLON.toCode())
                .append(TokenType.NEWLINE.toCode())
                .append(statements.stream()
                        .map(s -> s.toCode(indentationLevel + 1, false))
                        .collect(Collectors.joining())
                )
                .append(TokenType.END.toCode(indentationLevel, false))
                .append(TokenType.NEWLINE.toCode())
                .toString();
      }

      @Override
      public String toCode(int indentationLevel, boolean ignoreFirst) {
          return toCode(indentationLevel, ignoreFirst, null);
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
      public String toCode(int indentationLevel, boolean ignoreFirst) {
        return TokenType.BANG_BANG.toCode(indentationLevel, ignoreFirst)
                  + tokens.stream()
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
    public String toCode(int indentationLevel, boolean ignoreFirst) {
      return new StringBuilder()
              .append(annotations != null
                      ? annotations.stream()
                          .map(a -> a.toCode(indentationLevel, false))
                          .collect(Collectors.joining(TokenType.NEWLINE.toCode()))
                      : ""
              )
              .append(Codifiable.getIndentation(indentationLevel))
              .append(name.lexeme)
              .append(" ").append(TokenType.EQUAL.toCode()).append(" ")
              .append(value.toCode(0, false))
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
    public String toCode(int indentationLevel, boolean ignoreFirst) {
      return new StringBuilder(TokenType.LEFT_BRACKET.toCode(indentationLevel, ignoreFirst))
              .append(assigns.stream()
                      .map(a -> a.name.lexeme)
                      .collect(Collectors.joining(TokenType.COMMA.toCode() + " "))
              )
              .append(TokenType.RIGHT_BRACKET.toCode())
              .append(" ").append(TokenType.EQUAL.toCode()).append(" ")
              .append(((Get) assigns.get(0).value).object.toCode(0, false))
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
    public String toCode(int indentationLevel, boolean ignoreFirst) {
      return left.toCode(indentationLevel, ignoreFirst) + " " + operator.type.toCode() + " " + right.toCode(0, false);
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
    public String toCode(int indentationLevel, boolean ignoreFirst) {
      return new StringBuilder(callee.toCode(indentationLevel, ignoreFirst))
              .append(TokenType.LEFT_PAREN.toCode())
              .append(arguments.stream()
                      .map(a -> a.toCode(0, false))
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
    public String toCode(int indentationLevel, boolean ignoreFirst) {
      return object.toCode(indentationLevel, ignoreFirst) + TokenType.DOT.toCode() + name.toCode(0, false);
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
    public String toCode(int indentationLevel, boolean ignoreFirst) {
      return new StringBuilder(TokenType.LEFT_PAREN.toCode(indentationLevel, ignoreFirst))
              .append(" ")
              .append(expression.toCode(0, false))
              .append(" ")
              .append(TokenType.RIGHT_PAREN.toCode())
              .toString();
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
    public String toCode(int indentationLevel, boolean ignoreFirst) {
      return new StringBuilder(TokenType.GU.toCode(indentationLevel, ignoreFirst))
              .append(" ")
              .append(expr.toCode(0, false))
              .toString();
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
    public String toCode(int indentationLevel, boolean ignoreFirst) {
      return new StringBuilder(TokenType.IVIC.toCode(indentationLevel, ignoreFirst))
            .append(" ")
            .append(expr.toCode(0, false))
            .toString();
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
    public String toCode(int indentationLevel, boolean ignoreFirst) {
      return value.toCode(indentationLevel, ignoreFirst);
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
    public String toCode(int indentationLevel, boolean ignoreFirst) {
      return new StringBuilder(left.toCode(indentationLevel, ignoreFirst))
              .append(" ")
              .append(operator.type.toCode())
              .append(" ")
              .append(left.toCode(0, false))
              .toString();
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
    public String toCode(int indentationLevel, boolean ignoreFirst) {
      return new StringBuilder(object.toCode(indentationLevel, ignoreFirst))
              .append(TokenType.DOT.toCode())
              .append(name.toCode(0, false))
              .append(" ").append(TokenType.EQUAL.toCode()).append(" ")
              .append(value.toCode(0, false))
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
    public String toCode(int indentationLevel, boolean ignoreFirst) {
      StringBuilder sb = new StringBuilder(keyword.type.toCode(indentationLevel, ignoreFirst));
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
    public String toCode(int indentationLevel, boolean ignoreFirst) {
      return keyword.type.toCode(indentationLevel, ignoreFirst);
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
    public String toCode(int indentationLevel, boolean ignoreFirst) {
      return  operator.type.toCode(indentationLevel, ignoreFirst) + " " + right.toCode(0, false);
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
      public String toCode(int indentationLevel, boolean ignoreFirst) {
        return (ignoreFirst ? "" : Codifiable.getIndentation(indentationLevel)) + name.lexeme;
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
      public String toCode(int indentationLevel, boolean ignoreFirst) {
        return new StringBuilder(opener.type.toCode(indentationLevel, ignoreFirst))
                .append(isDictionary ? TokenType.NEWLINE.toCode() : "")
                .append(props.stream()
                  .map(p -> p.toCode(isDictionary ? indentationLevel + 1 : 0, false))
                  .collect(Collectors.joining(TokenType.COMMA.toCode() + (isDictionary ? TokenType.NEWLINE.toCode() : " ")))
                )
                .append(isDictionary ? TokenType.NEWLINE.toCode() : "")
                .append(TokenType.RIGHT_BRACKET.toCode(indentationLevel, false))
                .toString();
      }
    }
}
