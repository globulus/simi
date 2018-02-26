package net.globulus.simi;

import net.globulus.simi.api.SimiBlock;
import net.globulus.simi.api.SimiStatement;
import net.globulus.simi.api.SimiValue;

import java.util.List;

abstract class Expr {

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

        final Token declaration;
        final List<Token> params;
        final List<Stmt> statements;

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
    }

    static class Annotations extends Expr {
      Annotations(List<Token> tokens) {
        this.tokens = tokens;
      }

      <R> R accept(Visitor<R> visitor, Object... params) {
        return visitor.visitAnnotationsExpr(this);
      }

      final List<Token> tokens;
    }

  static class Assign extends Expr {
    Assign(Token name, Expr value) {
      this.name = name;
      this.value = value;
    }

    <R> R accept(Visitor<R> visitor, Object... params) {
      return visitor.visitAssignExpr(this);
    }

    final Token name;
    final Expr value;
  }

  static class Binary extends Expr {
    Binary(Expr left, Token operator, Expr right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }

    <R> R accept(Visitor<R> visitor, Object... params) {
      return visitor.visitBinaryExpr(this);
    }

    final Expr left;
    final Token operator;
    final Expr right;
  }

  static class Call extends Expr {
    Call(Expr callee, Token paren, List<Expr> arguments) {
      this.callee = callee;
      this.paren = paren;
      this.arguments = arguments;
    }

    <R> R accept(Visitor<R> visitor, Object... params) {
      return visitor.visitCallExpr(this);
    }

    final Expr callee;
    final Token paren;
    final List<Expr> arguments;
  }

  static class Get extends Expr {
    Get(Token origin, Expr object, Expr name, Integer arity) {
      this.origin = origin;
      this.object = object;
      this.name = name;
      this.arity = arity;
    }

    <R> R accept(Visitor<R> visitor, Object... params) {
      return visitor.visitGetExpr(this);
    }

    final Token origin;
    final Expr object;
    final Expr name;
    final Integer arity;
  }

  static class Grouping extends Expr {
    Grouping(Expr expression) {
      this.expression = expression;
    }

    <R> R accept(Visitor<R> visitor, Object... params) {
      return visitor.visitGroupingExpr(this);
    }

    final Expr expression;
  }

  static class Gu extends Expr {
    Gu(Expr.Literal string) {
      this.string = string;
    }

    <R> R accept(Visitor<R> visitor, Object... params) {
      return visitor.visitGuExpr(this);
    }

    final Expr.Literal string;
  }

  static class Literal extends Expr {
    Literal(SimiValue value) {
      this.value = value;
    }

    <R> R accept(Visitor<R> visitor, Object... params) {
      return visitor.visitLiteralExpr(this);
    }

    final SimiValue value;
  }

  static class Logical extends Expr {
    Logical(Expr left, Token operator, Expr right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }

    <R> R accept(Visitor<R> visitor, Object... params) {
      return visitor.visitLogicalExpr(this);
    }

    final Expr left;
    final Token operator;
    final Expr right;
  }

  static class Set extends Expr {
    Set(Token origin, Expr object, Expr name, Expr value) {
      this.origin = origin;
      this.object = object;
      this.name = name;
      this.value = value;
    }

    <R> R accept(Visitor<R> visitor, Object... params) {
      return visitor.visitSetExpr(this);
    }

    final Token origin;
    final Expr object;
    final Expr name;
    final Expr value;
  }

  static class Super extends Expr {
    Super(Token keyword, Token superclass, Token method, Integer arity) {
      this.keyword = keyword;
      this.superclass = superclass;
      this.method = method;
      this.arity = arity;
    }

    <R> R accept(Visitor<R> visitor, Object... params) {
      return visitor.visitSuperExpr(this);
    }

    final Token keyword;
    final Token superclass;
    final Token method;
    final Integer arity;
  }
  static class Self extends Expr {
    Self(Token keyword) {
      this.keyword = keyword;
    }

    <R> R accept(Visitor<R> visitor, Object... params) {
      return visitor.visitSelfExpr(this);
    }

    final Token keyword;
  }
  static class Unary extends Expr {
    Unary(Token operator, Expr right) {
      this.operator = operator;
      this.right = right;
    }

    <R> R accept(Visitor<R> visitor, Object... params) {
      return visitor.visitUnaryExpr(this);
    }

    final Token operator;
    final Expr right;
  }
    static class Variable extends Expr {
        Variable(Token name) {
            this.name = name;
        }

        <R> R accept(Visitor<R> visitor, Object... params) {
            return visitor.visitVariableExpr(this);
        }

        final Token name;
    }

    static class ObjectLiteral extends Expr {
        ObjectLiteral(Token opener, List<Expr> props, boolean isDictionary) {
            this.opener = opener;
            this.props = props;
            this.isDictionary = isDictionary;
        }

        <R> R accept(Visitor<R> visitor, Object... params) {
            return visitor.visitObjectLiteralExpr(this);
        }

        final Token opener;
        final List<Expr> props;
        final boolean isDictionary;
    }
}
