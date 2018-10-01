package net.globulus.simi;

import net.globulus.simi.api.Codifiable;
import net.globulus.simi.api.SimiStatement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

abstract class Stmt implements SimiStatement, Codifiable {

  abstract <R> R accept(Visitor<R> visitor, Object... args);

  @Override
  public int hashCode() {
    return super.hashCode() + toCode(0, true).hashCode();
  }

  interface Visitor<R> {
    R visitAnnotationStmt(Annotation stmt);
    R visitBreakStmt(Break stmt);
    R visitClassStmt(Class stmt, boolean addToEnv);
    R visitContinueStmt(Continue stmt);
    R visitElsifStmt(Elsif stmt);
    R visitExpressionStmt(Expression stmt);
    R visitFunctionStmt(Function stmt);
    R visitForStmt(For stmt);
    R visitIfStmt(If stmt);
    R visitImportStmt(Import stmt);
    R visitPrintStmt(Print stmt);
    R visitRescueStmt(Rescue stmt);
    R visitReturnStmt(Return stmt);
    R visitWhileStmt(While stmt);
    R visitYieldStmt(Yield stmt);
  }

  interface BlockStmt {
    List<BlockStmt> getChildren();
  }

  static class Annotation extends Stmt {

    final Expr expr;

    Annotation(Expr expr) {
      this.expr = expr;
    }

    <R> R accept(Visitor<R> visitor, Object... args) {
      return visitor.visitAnnotationStmt(this);
    }

    @Override
    public String toCode(int indentationLevel, boolean ignoreFirst) {
      return TokenType.BANG.toCode(indentationLevel, false) + expr.toCode(indentationLevel, true) + TokenType.NEWLINE.toCode();
    }

    @Override
    public int getLineNumber() {
      return expr.getLineNumber();
    }

    @Override
    public String getFileName() {
      return expr.getFileName();
    }

    @Override
    public boolean hasBreakPoint() {
      return false;
    }
  }

  static class Break extends Stmt {

    final Token name;

    Break(Token name) {
      this.name = name;
    }

    <R> R accept(Visitor<R> visitor, Object... args) {
      return visitor.visitBreakStmt(this);
    }

    @Override
    public String toCode(int indentationLevel, boolean ignoreFirst) {
      return name.type.toCode(indentationLevel, false) + TokenType.NEWLINE.toCode();
    }

    @Override
    public int getLineNumber() {
      return name.line;
    }

    @Override
    public String getFileName() {
      return name.file;
    }

    @Override
    public boolean hasBreakPoint() {
      return name.hasBreakpoint;
    }
  }

  static class Class extends Stmt {

    final Token opener;
    final Token name;
    final List<Expr> superclasses;
    final List<Expr> mixins;
    final List<Expr.Assign> constants;
    final List<Stmt.Class> innerClasses;
    final List<Stmt.Function> methods;
    final List<Stmt.Annotation> annotations;

    Class(Token opener, Token name, List<Expr> superclasses,
          List<Expr> mixins, List<Expr.Assign> constants,
          List<Stmt.Class> innerClasses, List<Stmt.Function> methods,
          List<Stmt.Annotation> annotations) {
      this.opener = opener;
      this.name = name;
      this.superclasses = superclasses;
      this.mixins = mixins;
      this.constants = constants;
      this.innerClasses = innerClasses;
      this.methods = methods;
      this.annotations = annotations;
    }

    <R> R accept(Visitor<R> visitor, Object... args) {
      boolean addToEnv = true;
      if (args != null && args.length > 0) {
        addToEnv = (Boolean) args[0];
      }
      return visitor.visitClassStmt(this, addToEnv);
    }

    @Override
    public String toCode(int indentationLevel, boolean ignoreFirst) {
      // TODO add annotations - can't add them now as the order of statements is unknown
      return new StringBuilder(opener.type.toCode(indentationLevel, false))
              .append(" ").append(name.lexeme)
              .append(superclasses != null
                      ? TokenType.LEFT_PAREN.toCode() + superclasses.stream()
                            .map(s -> s.toCode(0, false))
                            .collect(Collectors.joining(TokenType.COMMA.toCode() + " ")) + TokenType.RIGHT_PAREN.toCode()
                      : ""
              )
              .append(TokenType.COLON.toCode())
              .append(TokenType.NEWLINE.toCode())
              .append(mixins.stream()
                      .map(m -> TokenType.IMPORT.toCode(indentationLevel + 1, false) + " " + m.toCode(0, false))
                      .collect(Collectors.joining(TokenType.NEWLINE.toCode()))
              )
              .append(constants.stream().map(c -> c.toCode(indentationLevel + 1, false)).collect(Collectors.joining(TokenType.NEWLINE.toCode())))
              .append(TokenType.NEWLINE.toCode())
              .append(methods.stream().map(m -> m.toCode(indentationLevel + 1, false)).collect(Collectors.joining()))
              .append(TokenType.NEWLINE.toCode())
              .append(innerClasses.stream().map(i -> i.toCode(indentationLevel + 1, false)).collect(Collectors.joining()))
              .append(TokenType.NEWLINE.toCode())
              .append(TokenType.END.toCode(indentationLevel, false))
              .append(TokenType.NEWLINE.toCode())
              .toString();
    }

    @Override
    public int getLineNumber() {
      return opener.line;
    }

    @Override
    public String getFileName() {
      return opener.file;
    }

    @Override
    public boolean hasBreakPoint() {
      return opener.hasBreakpoint;
    }
  }

  static class Continue extends Stmt {

    final Token name;

    Continue(Token name) {
      this.name = name;
    }

    <R> R accept(Visitor<R> visitor, Object... args) {
      return visitor.visitContinueStmt(this);
    }

    @Override
    public String toCode(int indentationLevel, boolean ignoreFirst) {
      return name.type.toCode(indentationLevel, false) + TokenType.NEWLINE.toCode();
    }

    @Override
    public int getLineNumber() {
      return name.line;
    }

    @Override
    public String getFileName() {
      return name.file;
    }

    @Override
    public boolean hasBreakPoint() {
      return name.hasBreakpoint;
    }
  }

  static class Expression extends Stmt {

    final Expr expression;

    Expression(Expr expression) {
      this.expression = expression;
    }

    <R> R accept(Visitor<R> visitor, Object... args) {
      return visitor.visitExpressionStmt(this);
    }

    @Override
    public String toCode(int indentationLevel, boolean ignoreFirst) {
      return expression.toCode(indentationLevel, false) + TokenType.NEWLINE.toCode();
    }

    @Override
    public int getLineNumber() {
      return expression.getLineNumber();
    }

    @Override
    public String getFileName() {
      return expression.getFileName();
    }

    @Override
    public boolean hasBreakPoint() {
      return expression.hasBreakPoint();
    }
  }

  static class Function extends Stmt {

    final Token name;
    final Expr.Block block;
    final List<Stmt.Annotation> annotations;

    Function(Token name, Expr.Block block, List<Stmt.Annotation> annotations) {
      this.name = name;
      this.block = block;
      this.annotations = annotations;
    }

    <R> R accept(Visitor<R> visitor, Object... args) {
      return visitor.visitFunctionStmt(this);
    }

    @Override
    public String toCode(int indentationLevel, boolean ignoreFirst) {
      return new StringBuilder()
              .append(annotations != null
                      ? annotations.stream().map(a -> a.toCode(indentationLevel, false)).collect(Collectors.joining())
                      : "")
              .append(block.toCode(indentationLevel, false, name.lexeme))
              .toString();
    }

    @Override
    public int getLineNumber() {
      return name.line;
    }

    @Override
    public String getFileName() {
      return name.file;
    }

    @Override
    public boolean hasBreakPoint() {
      return name.hasBreakpoint;
    }
  }

    static class Elsif extends Stmt implements BlockStmt {

      final Expr condition;
      final Expr.Block thenBranch;

        Elsif(Expr condition, Expr.Block thenBranch) {
            this.condition = condition;
            this.thenBranch = thenBranch;
        }

        <R> R accept(Visitor<R> visitor, Object... args) {
            return visitor.visitElsifStmt(this);
        }

      @Override
      public List<BlockStmt> getChildren() {
        return thenBranch.getStatements().stream()
                .filter(s -> s instanceof BlockStmt)
                .map(s -> (BlockStmt) s)
                .collect(Collectors.toList());
      }

      @Override
      public String toCode(int indentationLevel, boolean ignoreFirst) {
        return condition.toCode(indentationLevel, ignoreFirst) + thenBranch.toCode(indentationLevel, true);
      }

      @Override
      public int getLineNumber() {
        return condition.getLineNumber();
      }

      @Override
      public String getFileName() {
        return condition.getFileName();
      }

      @Override
      public boolean hasBreakPoint() {
        return condition.hasBreakPoint();
      }
    }

  static class If extends Stmt implements BlockStmt {

    final Elsif ifstmt;
    final List<Elsif> elsifs;
    final Expr.Block elseBranch;

    If(Elsif ifstmt, List<Elsif> elsifs, Expr.Block elseBranch) {
        this.ifstmt = ifstmt;
        this.elsifs = elsifs;
      this.elseBranch = elseBranch;
    }

    @Override
    <R> R accept(Visitor<R> visitor, Object... args) {
      return visitor.visitIfStmt(this);
    }

    @Override
    public List<BlockStmt> getChildren() {
      List<BlockStmt> children = new ArrayList<>();
      children.add(ifstmt);
      children.addAll(elsifs);
      if (elseBranch != null) {
        children.addAll(elseBranch.getStatements().stream()
                .filter(s -> s instanceof BlockStmt)
                .map(s -> (BlockStmt) s)
                .collect(Collectors.toList()));
      }
      return children;
    }

    @Override
    public String toCode(int indentationLevel, boolean ignoreFirst) {
      return new StringBuilder(TokenType.IF.toCode(indentationLevel, false))
              .append(" ")
              .append(ifstmt.toCode(indentationLevel, true))
              .append(elsifs.stream().map(e -> TokenType.ELSIF.toCode() + " " + e.toCode(indentationLevel, true)).collect(Collectors.joining()))
              .append(elseBranch != null ? TokenType.ELSE.toCode(indentationLevel, false) + elseBranch.toCode(indentationLevel, true) : "")
              .toString();
    }

    @Override
    public int getLineNumber() {
      return ifstmt.getLineNumber();
    }

    @Override
    public String getFileName() {
      return ifstmt.getFileName();
    }

    @Override
    public boolean hasBreakPoint() {
      return ifstmt.hasBreakPoint();
    }
  }

  static class Print extends Stmt {

    final Expr expression;

    Print(Expr expression) {
      this.expression = expression;
    }

    <R> R accept(Visitor<R> visitor, Object... args) {
      return visitor.visitPrintStmt(this);
    }

    @Override
    public String toCode(int indentationLevel, boolean ignoreFirst) {
      return TokenType.PRINT.toCode(indentationLevel, false) + " " + expression.toCode(0, false) + TokenType.NEWLINE.toCode();
    }

    @Override
    public int getLineNumber() {
      return expression.getLineNumber();
    }

    @Override
    public String getFileName() {
      return expression.getFileName();
    }

    @Override
    public boolean hasBreakPoint() {
      return expression.hasBreakPoint();
    }
  }

  static class Rescue extends Stmt {

    final Token keyword;
    final Expr.Block block;

    Rescue(Token keyword, Expr.Block block) {
      this.keyword = keyword;
      this.block = block;
    }

    <R> R accept(Visitor<R> visitor, Object... args) {
      return visitor.visitRescueStmt(this);
    }

    @Override
    public String toCode(int indentationLevel, boolean ignoreFirst) {
      return keyword.type.toCode() + " " + block.toCode(indentationLevel, true);
    }

    @Override
    public int getLineNumber() {
      return keyword.line;
    }

    @Override
    public String getFileName() {
      return keyword.file;
    }

    @Override
    public boolean hasBreakPoint() {
      return keyword.hasBreakpoint;
    }
  }

  static class Import extends Stmt {

    final Token keyword;
    final Expr value;

    Import(Token keyword, Expr value) {
      this.keyword = keyword;
      this.value = value;
    }

    <R> R accept(Visitor<R> visitor, Object... args) {
      return visitor.visitImportStmt(this);
    }

    @Override
    public String toCode(int indentationLevel, boolean ignoreFirst) {
      return keyword.type.toCode(indentationLevel, false) + " " + value.toCode(0, false) + TokenType.NEWLINE.toCode();
    }

    @Override
    public int getLineNumber() {
      return keyword.line;
    }

    @Override
    public String getFileName() {
      return keyword.file;
    }

    @Override
    public boolean hasBreakPoint() {
      return keyword.hasBreakpoint;
    }
  }

  static class Return extends Stmt {

    final Token keyword;
    final Expr value;

    Return(Token keyword, Expr value) {
      this.keyword = keyword;
      this.value = value;
    }

    <R> R accept(Visitor<R> visitor, Object... args) {
      return visitor.visitReturnStmt(this);
    }

    @Override
    public String toCode(int indentationLevel, boolean ignoreFirst) {
      return keyword.type.toCode(indentationLevel, false) + " "
              + ((value != null) ? value.toCode(0, false) : TempNull.INSTANCE.toCode(0, false))
              + TokenType.NEWLINE.toCode();
    }

    @Override
    public int getLineNumber() {
      return keyword.line;
    }

    @Override
    public String getFileName() {
      return keyword.file;
    }

    @Override
    public boolean hasBreakPoint() {
      return keyword.hasBreakpoint;
    }
  }

  static class While extends Stmt implements BlockStmt {

    final Expr condition;
    final Expr.Block body;

    While(Expr condition, Expr.Block body) {
      this.condition = condition;
      this.body = body;
    }

    <R> R accept(Visitor<R> visitor, Object... args) {
      return visitor.visitWhileStmt(this);
    }

    @Override
    public List<BlockStmt> getChildren() {
      return body.getStatements().stream()
              .filter(s -> s instanceof BlockStmt)
              .map(s -> (BlockStmt) s)
              .collect(Collectors.toList());
    }

    @Override
    public String toCode(int indentationLevel, boolean ignoreFirst) {
      return new StringBuilder(TokenType.WHILE.toCode())
              .append(" ")
              .append(condition.toCode(indentationLevel, false))
              .append(body.toCode(indentationLevel, true))
              .toString();
    }

    @Override
    public int getLineNumber() {
      return condition.getLineNumber();
    }

    @Override
    public String getFileName() {
      return condition.getFileName();
    }

    @Override
    public boolean hasBreakPoint() {
      return condition.hasBreakPoint();
    }
  }

  static class For extends Stmt implements BlockStmt {

    final Expr.Variable var;
    final Expr iterable;
    final Expr.Block body;

      For(Expr.Variable var, Expr iterable, Expr.Block body) {
        this.var = var;
        this.iterable = iterable;
        this.body = body;
      }

      <R> R accept(Visitor<R> visitor, Object... args) {
          return visitor.visitForStmt(this);
      }

    @Override
    public List<BlockStmt> getChildren() {
      return body.getStatements().stream()
              .filter(s -> s instanceof BlockStmt)
              .map(s -> (BlockStmt) s)
              .collect(Collectors.toList());
    }

    @Override
    public String toCode(int indentationLevel, boolean ignoreFirst) {
      return new StringBuilder(TokenType.FOR.toCode(indentationLevel, false))
              .append(" ")
              .append(var.toCode(0, false))
              .append(" ").append(TokenType.IN.toCode()).append(" ")
              .append(iterable.toCode(0, false))
              .append(body.toCode(indentationLevel, true))
              .toString();
    }

    @Override
    public int getLineNumber() {
      return var.getLineNumber();
    }

    @Override
    public String getFileName() {
      return var.getFileName();
    }

    @Override
    public boolean hasBreakPoint() {
      return var.hasBreakPoint();
    }
  }

  static class Yield extends Stmt {

    final Token keyword;
    final Expr value;

    Yield(Token keyword, Expr value) {
      this.keyword = keyword;
      this.value = value;
    }

    <R> R accept(Visitor<R> visitor, Object... args) {
      return visitor.visitYieldStmt(this);
    }

    @Override
    public String toCode(int indentationLevel, boolean ignoreFirst) {
      return keyword.type.toCode(indentationLevel, false) + " " + value.toCode(0, false) + TokenType.NEWLINE.toCode();
    }

    @Override
    public int getLineNumber() {
      return keyword.line;
    }

    @Override
    public String getFileName() {
      return keyword.file;
    }

    @Override
    public boolean hasBreakPoint() {
      return keyword.hasBreakpoint;
    }
  }
}
