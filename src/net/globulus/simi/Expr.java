package net.globulus.simi;

import net.globulus.simi.api.Codifiable;
import net.globulus.simi.api.SimiBlock;
import net.globulus.simi.api.SimiStatement;
import net.globulus.simi.api.SimiValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

abstract class Expr implements Codifiable {

  abstract <R> R accept(Visitor<R> visitor, Object... params);

  interface Visitor<R> {
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
    R visitElsifExpr(Elsif expr);
    R visitIfExpr(If expr);
  }
    static class Block extends Expr implements SimiBlock {

      final Token declaration;
      final List<Expr> params;
      final List<Stmt> statements;
      final boolean canReturn;
      final boolean isNative;

      private final List<Stmt> processedStatements;

        Block(Token declaration,
              List<Expr> params,
              List<Stmt> statements,
              boolean canReturn) {
            this.declaration = declaration;
            this.params = params;
            this.statements = statements;
            this.canReturn = canReturn;

            boolean isNative = false;
            if (statements.size() == 1) {
              Stmt stmt = statements.get(0);
              Expr expr = null;
              if (stmt instanceof Stmt.Expression) {
                expr = ((Stmt.Expression) stmt).expression;
              } else if (stmt instanceof Stmt.Return) {
                expr = ((Stmt.Return) stmt).value;
              }
              if (expr instanceof Expr.Literal && ((Literal) expr).value instanceof Native) {
                isNative = true;
              }
            }

            this.isNative = isNative;
            processedStatements = processStatements();
        }

        <R> R accept(Visitor<R> visitor, Object... params) {
            boolean newScope = (params.length < 1) ? true : (Boolean) params[0];
            boolean execute = (params.length < 2) ? true : (Boolean) params[1];
            return visitor.visitBlockExpr(this, newScope, execute);
        }

      @Override
      public List<? extends SimiStatement> getStatements() {
        return processedStatements;
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
        Expr expr = ((Stmt.Expression) stmt).expression;
        if (!(expr instanceof Expr.Literal)) {
          return false;
        }
        return ((Expr.Literal) expr).value instanceof Pass;
      }

      String toCode(int indentationLevel, boolean ignoreFirst, String name) {
        String opener;
        if (declaration.type == TokenType.DEF) {
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
                .map(BlockImpl::getParamLexeme)
                .collect(Collectors.joining(TokenType.COMMA.toCode() + " "))
        );
        if (needsParenthesis) {
          paramsBuilder.append(TokenType.RIGHT_PAREN.toCode());
        }
        return new StringBuilder(ignoreFirst ? "" : Codifiable.getIndentation(indentationLevel))
                .append(opener)
                .append(paramsBuilder.toString())
                .append(TokenType.LEFT_BRACE.toCode())
                .append(TokenType.NEWLINE.toCode())
                .append(statements.stream()
                        .map(s -> s.toCode(indentationLevel + 1, false))
                        .collect(Collectors.joining())
                )
                .append(TokenType.RIGHT_BRACE.toCode(indentationLevel, false))
                .append(TokenType.NEWLINE.toCode())
                .toString();
      }

      @Override
      public String toCode(int indentationLevel, boolean ignoreFirst) {
          return toCode(indentationLevel, ignoreFirst, null);
      }

      @Override
      public int getLineNumber() {
        return declaration.line;
      }

      @Override
      public String getFileName() {
        return declaration.file;
      }

      @Override
        public boolean hasBreakPoint() {
            return declaration.hasBreakpoint;
        }

        private List<Stmt> processStatements() {
          List<Stmt> localStatements = new ArrayList<>();
          final int size = statements.size();
          for (int i = 0; i < size; i++) {
            Stmt stmt = statements.get(i);
            if (stmt instanceof Stmt.Expression && ((Stmt.Expression) stmt).expression instanceof Yield) {
              Yield expr = (Yield) ((Stmt.Expression) stmt).expression;
              Variable response = new Variable(Token.named("response_" + System.currentTimeMillis() + "_" + Math.abs(new Random().nextLong())));
              Stmt assignment = new Stmt.Expression(Parser.getAssignExpr(null, expr.var, expr.assign, response));
              List<Stmt> otherStmts = new ArrayList<>(size - i + 1);
              otherStmts.add(assignment);
              otherStmts.addAll(statements.subList(i + 1, size));
              Expr callee = expr.value.callee;
              if (callee instanceof Expr.Get) {
                Expr.Get get = (Expr.Get) callee;
                callee = new Expr.Get(get.origin, get.object, get.name, get.arity + 1);
              }
              Expr.Call call = new Expr.Call(callee, expr.value.paren, new ArrayList<>(expr.value.arguments));
              call.arguments.add(new Expr.Block(
                      new Token(TokenType.DEF, null, null, expr.assign.line, expr.assign.file),
                      Collections.singletonList(response),
                      otherStmts,
                      true));
              localStatements.add(new Stmt.Expression(call));
              break;
            } else {
              localStatements.add(stmt);
            }
          }
          return localStatements;
      }
    }

  static class Assign extends Expr {

    final Token name;
    final Token operator;
    final Expr value;
    final List<Stmt.Annotation> annotations;

    Assign(Token name, Token operator, Expr value, List<Stmt.Annotation> annotations) {
      this.name = name;
      this.operator = operator;
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
              .append(" ").append(operator.type.toCode()).append(" ")
              .append(value.toCode(0, false))
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
              .append(((Get)((Binary) assigns.get(0).value).left).object.toCode(0, false))
              .toString();
    }

    @Override
    public int getLineNumber() {
      return assigns.get(0).getLineNumber();
    }

    @Override
    public String getFileName() {
      return assigns.get(0).getFileName();
    }

    @Override
      public boolean hasBreakPoint() {
          return assigns.get(0).hasBreakPoint();
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

    @Override
    public int getLineNumber() {
      return operator.line;
    }

    @Override
    public String getFileName() {
      return operator.file;
    }

    @Override
      public boolean hasBreakPoint() {
          return operator.hasBreakpoint;
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
              .append(paren.type.toCode())
              .append(arguments.stream()
                      .map(a -> a.toCode(0, false))
                      .collect(Collectors.joining(TokenType.COMMA.toCode() + " "))
              )
              .append(TokenType.RIGHT_PAREN.toCode())
              .toString();
    }

    @Override
    public int getLineNumber() {
      return paren.line;
    }

    @Override
    public String getFileName() {
      return paren.file;
    }

    @Override
      public boolean hasBreakPoint() {
          return paren.hasBreakpoint;
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

    @Override
    public int getLineNumber() {
      return origin.line;
    }

    @Override
    public String getFileName() {
      return origin.file;
    }

    @Override
      public boolean hasBreakPoint() {
          return origin.hasBreakpoint;
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
          return expr.hasBreakPoint();
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
          return expr.hasBreakPoint();
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
      if (this.value == null) {
        return TokenType.NIL.toCode(indentationLevel, ignoreFirst);
      }
      return value.toCode(indentationLevel, ignoreFirst);
    }

    @Override
    public int getLineNumber() {
      return (value == null) ? -1 : value.getLineNumber();
    }

    @Override
    public String getFileName() {
      return (value == null) ? null : value.getFileName();
    }

    @Override
      public boolean hasBreakPoint() {
          return (value != null) && value.hasBreakPoint();
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
              .append(right.toCode(0, false))
              .toString();
    }

    @Override
    public int getLineNumber() {
      return operator.line;
    }

    @Override
    public String getFileName() {
      return operator.file;
    }

    @Override
      public boolean hasBreakPoint() {
          return operator.hasBreakpoint;
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

    @Override
    public int getLineNumber() {
      return origin.line;
    }

    @Override
    public String getFileName() {
      return origin.file;
    }

    @Override
      public boolean hasBreakPoint() {
          return origin.hasBreakpoint;
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

  static class Self extends Expr {

    final Token keyword;
    final Token specifier;

    Self(Token keyword, Token specifier) {
      this.keyword = keyword;
      this.specifier = specifier;
    }

    <R> R accept(Visitor<R> visitor, Object... params) {
      return visitor.visitSelfExpr(this);
    }

    @Override
    public String toCode(int indentationLevel, boolean ignoreFirst) {
      StringBuilder sb = new StringBuilder(keyword.type.toCode(indentationLevel, ignoreFirst));
      if (specifier != null) {
        sb.append(TokenType.LEFT_PAREN.toCode())
                .append(specifier.type.toCode())
                .append(TokenType.RIGHT_PAREN.toCode());
      }
      return sb.toString();
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
      String space = (operator.type == TokenType.MINUS
              || operator.type == TokenType.QUESTION
              || operator.type == TokenType.BANG_BANG) ? "" : " ";
      return  operator.type.toCode(indentationLevel, ignoreFirst) + space + right.toCode(0, false);
    }

    @Override
    public int getLineNumber() {
      return operator.line;
    }

    @Override
    public String getFileName() {
      return operator.file;
    }

    @Override
      public boolean hasBreakPoint() {
          return operator.hasBreakpoint;
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

    static class ObjectLiteral extends Expr {

      final Token opener;
      final List<Expr> props;

        ObjectLiteral(Token opener, List<Expr> props) {
            this.opener = opener;
            this.props = props;
        }

        <R> R accept(Visitor<R> visitor, Object... params) {
            return visitor.visitObjectLiteralExpr(this);
        }

      @Override
      public String toCode(int indentationLevel, boolean ignoreFirst) {
        return new StringBuilder(opener.type.toCode(indentationLevel, ignoreFirst))
                .append(props.stream()
                  .map(p -> p.toCode(indentationLevel + 1, false))
                  .collect(Collectors.joining(TokenType.COMMA.toCode() + TokenType.NEWLINE.toCode()))
                )
                .append(TokenType.RIGHT_BRACKET.toCode(indentationLevel, false))
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

  static class Yield extends Expr {

    final Expr var;
    final Token assign;
    final Token keyword;
    final Expr.Call value;

    Yield(Expr var, Token assign, Token keyword, Expr.Call value) {
      this.var = var;
      this.assign = assign;
      this.keyword = keyword;
      this.value = value;
    }

    <R> R accept(Visitor<R> visitor, Object... args) {
      return null;
    }

    @Override
    public String toCode(int indentationLevel, boolean ignoreFirst) {
      return var.toCode(indentationLevel, false)
              + " "
              + assign.type.toCode(0, false)
              + " "
              + keyword.type.toCode(0, false)
              + " "
              + value.toCode(0, false);
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

  static class Elsif extends Expr {

    final Expr condition;
    final Expr.Block thenBranch;

    Elsif(Expr condition, Expr.Block thenBranch) {
      this.condition = condition;
      this.thenBranch = thenBranch;
    }

    <R> R accept(Visitor<R> visitor, Object... args) {
      return visitor.visitElsifExpr(this);
    }

//    @Override
//    public List<BlockStmt> getChildren() {
//      return thenBranch.getStatements().stream()
//              .filter(s -> s instanceof BlockStmt)
//              .map(s -> (BlockStmt) s)
//              .collect(Collectors.toList());
//    }

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

  static class If extends Expr {

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
      return visitor.visitIfExpr(this);
    }

//    @Override
//    public List<BlockStmt> getChildren() {
//      List<BlockStmt> children = new ArrayList<>();
//      children.add(ifstmt);
//      children.addAll(elsifs);
//      if (elseBranch != null) {
//        children.addAll(elseBranch.getStatements().stream()
//                .filter(s -> s instanceof BlockStmt)
//                .map(s -> (BlockStmt) s)
//                .collect(Collectors.toList()));
//      }
//      return children;
//    }

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

  static boolean hasImplicitReturn(Expr expr) {
    if (expr instanceof Expr.Literal) {
      return !(((Literal) expr).value instanceof Native); // native methods don't have implicit return wrapping
    }
    if (expr instanceof Set) {
      return !(((Set) expr).object instanceof Expr.Self); // setter methods don't have implicit return wrapping
    }
    return true;
  }
}
