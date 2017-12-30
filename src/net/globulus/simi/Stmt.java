package net.globulus.simi;

import java.util.List;

abstract class Stmt {

  interface Visitor<R> {
    R visitClassStmt(Class stmt);
    R visitExpressionStmt(Expression stmt);
    R visitFunctionStmt(Function stmt);
    R visitElsifStmt(Elsif stmt);
    R visitIfStmt(If stmt);
    R visitPrintStmt(Print stmt);
    R visitReturnStmt(Return stmt);
    R visitWhileStmt(While stmt);
    R visitForStmt(For stmt);
  }

  static class Class extends Stmt {
    Class(Token name, List<Expr> superclasses, List<Expr.Assign> constants, List<Stmt.Function> methods) {
      this.name = name;
      this.superclasses = superclasses;
      this.constants = constants;
      this.methods = methods;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitClassStmt(this);
    }

    final Token name;
    final List<Expr> superclasses;
    final List<Expr.Assign> constants;
    final List<Stmt.Function> methods;
  }

  static class Expression extends Stmt {
    Expression(Expr expression) {
      this.expression = expression;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitExpressionStmt(this);
    }

    final Expr expression;
  }
//< stmt-expression

//> stmt-function
  static class Function extends Stmt {
    Function(Token declaration, Token name, Expr.Block block) {
        this.declaration = declaration;
      this.name = name;
      this.block = block;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitFunctionStmt(this);
    }

    final Token declaration;
    final Token name;
    final Expr.Block block;
  }
//< stmt-function

    static class Elsif extends Stmt {
        Elsif(Expr condition, Expr.Block thenBranch) {
            this.condition = condition;
            this.thenBranch = thenBranch;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitElsifStmt(this);
        }

        final Expr condition;
        final Expr.Block thenBranch;
    }

  static class If extends Stmt {
    If(Elsif ifstmt, List<Elsif> elsifs, Expr.Block elseBranch) {
        this.ifstmt = ifstmt;
        this.elsifs = elsifs;
      this.elseBranch = elseBranch;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitIfStmt(this);
    }

    final Elsif ifstmt;
    final List<Elsif> elsifs;
    final Expr.Block elseBranch;
  }

  static class Print extends Stmt {
    Print(Expr expression) {
      this.expression = expression;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitPrintStmt(this);
    }

    final Expr expression;
  }

  static class Return extends Stmt {
    Return(Token keyword, Expr value) {
      this.keyword = keyword;
      this.value = value;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitReturnStmt(this);
    }

    final Token keyword;
    final Expr value;
  }

  static class While extends Stmt {
    While(Expr condition, Expr.Block body) {
      this.condition = condition;
      this.body = body;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitWhileStmt(this);
    }

    final Expr condition;
    final Expr.Block body;
  }

    static class For extends Stmt {
        For(Token var, Stmt iterable, Expr.Block body) {
            this.var = var;
            this.iterable = iterable;
            this.body = body;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitForStmt(this);
        }

        final Token var;
        final Stmt iterable;
        final Expr.Block body;
    }

  abstract <R> R accept(Visitor<R> visitor);
}
