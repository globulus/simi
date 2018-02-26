package net.globulus.simi;

import net.globulus.simi.api.SimiStatement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

abstract class Stmt implements SimiStatement {

  interface Visitor<R> {
    R visitAnnotationStmt(Annotation stmt);
    R visitBreakStmt(Break stmt);
    R visitClassStmt(Class stmt);
    R visitContinueStmt(Continue stmt);
    R visitElsifStmt(Elsif stmt);
    R visitExpressionStmt(Expression stmt);
    R visitFunctionStmt(Function stmt);
    R visitForStmt(For stmt);
    R visitIfStmt(If stmt);
    R visitPrintStmt(Print stmt);
    R visitRescueStmt(Rescue stmt);
    R visitReturnStmt(Return stmt);
    R visitWhileStmt(While stmt);
    R visitYieldStmt(Yield stmt);
  }

  interface BlockStmt {
    List<BlockStmt> getChildren();
  }

  abstract <R> R accept(Visitor<R> visitor);

  static class Annotation extends Stmt {
    Annotation(Expr expr) {
      this.expr = expr;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitAnnotationStmt(this);
    }

    final Expr expr;
  }

  static class Break extends Stmt {
    Break(Token name) {
      this.name = name;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBreakStmt(this);
    }

    final Token name;
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

  static class Continue extends Stmt {
    Continue(Token name) {
      this.name = name;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitContinueStmt(this);
    }

    final Token name;
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

  static class Function extends Stmt {
    Function(Token name, Expr.Block block, List<Stmt.Annotation> annotations) {
      this.name = name;
      this.block = block;
      this.annotations = annotations;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitFunctionStmt(this);
    }

    final Token name;
    final Expr.Block block;
    final List<Stmt.Annotation> annotations;
  }

    static class Elsif extends Stmt implements BlockStmt {
        Elsif(Expr condition, Expr.Block thenBranch) {
            this.condition = condition;
            this.thenBranch = thenBranch;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitElsifStmt(this);
        }

        final Expr condition;
        final Expr.Block thenBranch;

      @Override
      public List<BlockStmt> getChildren() {
        return thenBranch.statements.stream()
                .filter(s -> s instanceof BlockStmt)
                .map(s -> (BlockStmt) s)
                .collect(Collectors.toList());
      }
    }

  static class If extends Stmt implements BlockStmt {
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

    @Override
    public List<BlockStmt> getChildren() {
      List<BlockStmt> children = new ArrayList<>();
      children.add(ifstmt);
      children.addAll(elsifs);
      if (elseBranch != null) {
        children.addAll(elseBranch.statements.stream()
                .filter(s -> s instanceof BlockStmt)
                .map(s -> (BlockStmt) s)
                .collect(Collectors.toList()));
      }
      return children;
    }
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

  static class Rescue extends Stmt {
    Rescue(Token keyword, Expr.Block block) {
      this.keyword = keyword;
      this.block = block;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitRescueStmt(this);
    }

    final Token keyword;
    final Expr.Block block;
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

  static class While extends Stmt implements BlockStmt {
    While(Expr condition, Expr.Block body) {
      this.condition = condition;
      this.body = body;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitWhileStmt(this);
    }

    final Expr condition;
    final Expr.Block body;

    @Override
    public List<BlockStmt> getChildren() {
      return body.statements.stream()
              .filter(s -> s instanceof BlockStmt)
              .map(s -> (BlockStmt) s)
              .collect(Collectors.toList());
    }
  }

  static class For extends Stmt implements BlockStmt {
      For(Expr.Variable var, Expr iterable, Expr.Block body) {
        this.var = var;
        this.iterable = iterable;
        this.body = body;
      }

      <R> R accept(Visitor<R> visitor) {
          return visitor.visitForStmt(this);
      }

      final Expr.Variable var;
      final Expr iterable;
      final Expr.Block body;

    @Override
    public List<BlockStmt> getChildren() {
      return body.statements.stream()
              .filter(s -> s instanceof BlockStmt)
              .map(s -> (BlockStmt) s)
              .collect(Collectors.toList());
    }
  }

  static class Yield extends Stmt {
    Yield(Token keyword, Expr value) {
      this.keyword = keyword;
      this.value = value;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitYieldStmt(this);
    }

    final Token keyword;
    final Expr value;
  }
}
