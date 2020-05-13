package net.globulus.simi

import net.globulus.simi.TokenType.*
import net.globulus.simi.api.SimiValue
import net.globulus.simi.tool.*
import java.util.*

internal class Compiler(private val tokens: List<Token>) {
    private lateinit var byteCode: MutableList<Byte>
    private var current = 0

    private val strTable = mutableMapOf<String, Int>()
    private val strList = mutableListOf<String>()
    private var strCount = 0

    private var sp = 0
    private val blocksStarts = mutableListOf<Int>()
    private val locals = mutableListOf<Local>()
    private var scopeDepth = -1 // Will be set to 0 with first beginScope()

    private var lastChunk: Chunk? = null
    private val chunks: Deque<Chunk> = LinkedList()

    fun compile(): CompilerOutput {
        byteCode = mutableListOf()
        beginScope()
        while (!isAtEnd()) {
//            if (match(TokenType.IMPORT)) {
//                val keyword: Token = previous()
//                if (match(TokenType.IDENTIFIER)) {
//                    statements.add(Stmt.Import(keyword, Expr.Variable(previous())))
//                } else { // String imports are handled during scanning phase
//                    continue
//                }
//            }
            if (match(NEWLINE, PASS)) {
                continue
            }
            declaration()
        }
        endScope()
        emitCode(OpCode.HALT)
        printChunks()
        return CompilerOutput(byteCode.toByteArray(), strList)
    }

    private fun beginScope() {
        emitCode(OpCode.PUSH_SCOPE)
        scopeDepth++
    }

    private fun endScope() {
        emitCode(OpCode.POP_SCOPE)
        scopeDepth--
    }

    private fun declaration() {
//        if (match(TokenType.CLASS, TokenType.CLASS_FINAL, TokenType.CLASS_OPEN)) {
//            return classDeclaration()
//        }
//        if (match(TokenType.DEF)) {
//            return function(Parser.KIND_FUNCTION)
//        }
//        if (match(TokenType.BANG)) {
//            return annotation()
//        }
//        if (match(TokenType.IMPORT)) {
//            val keyword = previous()
//            if (match(TokenType.IDENTIFIER)) {
//                return Stmt.Import(keyword, Expr.Variable(previous()))
//            }
//        }
        statement(false)
    }

    private fun statement(lambda: Boolean) {
        if (match(LEFT_BRACE)) {
            beginScope()
            block()
            endScope()
        }
        else if (match(IF)) {
            return ifSomething()
        }
//        if (match(TokenType.WHEN)) {
//            return whenStmt()
//        }
//        if (match(TokenType.FOR)) {
//            return forStatement()
//        }
        else if (match(PRINT)) {
            printStatement(lambda)
        }
//        if (match(TokenType.RETURN)) {
//            return returnStatement(lambda)
//        }
//        if (match(TokenType.WHILE)) {
//            return whileStatement()
//        }
//        if (match(TokenType.BREAK)) {
//            return breakStatement()
//        }
//        if (match(TokenType.CONTINUE)) {
//            return continueStatement()
//        }
//        if (match(TokenType.RESCUE)) {
//            return rescueStatement()
//        }
//        if (match(TokenType.YIELD)) {
//            return yieldStatement(lambda)
//        }
        else {
            expressionStatement(lambda)
        }
    }

    private fun block() {
        while (!check(RIGHT_BRACE)) {
            matchAllNewlines()
            declaration()
            matchAllNewlines()
        }
        consume(RIGHT_BRACE, "Expect '}' after block!")
    }

//    private fun classDeclaration(): Stmt.Class {
//        val opener = previous()
//        val name = consume(IDENTIFIER, "Expect class name.")
//        val mixins: MutableList<Expr> = ArrayList()
//        var superclasses: MutableList<Expr?>? = null
//        if (match(LEFT_PAREN)) {
//            if (!check(RIGHT_PAREN)) {
//                superclasses = ArrayList()
//                do {
//                    if (match(IN)) {
//                        mixins.add(call())
//                    } else {
//                        superclasses.add(call())
//                    }
//                } while (match(COMMA))
//            }
//            consume(RIGHT_PAREN, "Expect ')' after superclasses.")
//        }
//        val constants: MutableList<Assign> = ArrayList()
//        val innerClasses: MutableList<Stmt.Class> = ArrayList()
//        val methods: MutableList<Stmt.Function> = ArrayList()
//        if (match(LEFT_BRACE)) {
//            isInClassDeclr = true
//            while (!check(RIGHT_BRACE) && !isAtEnd()) {
//                if (match(NEWLINE)) {
//                    continue
//                }
//                if (match(DEF)) {
//                    methods.add(function(Parser.KIND_METHOD))
//                } else if (match(CLASS, CLASS_FINAL, CLASS_OPEN)) {
//                    innerClasses.add(classDeclaration())
//                } else if (match(BANG)) {
//                    annotations.add(annotation())
//                } else {
//                    val expr: Expr = assignment()
//                    if (expr is Assign) {
//                        constants.add(expr)
//                    }
//                }
//            }
//            isInClassDeclr = false
//            consume(RIGHT_BRACE, "Expect '}' after class body.")
//        } else {
//            consume(NEWLINE, "Expected newline after empty class declaration.")
//        }
//        return Class(opener, name, superclasses, mixins, constants, innerClasses, methods, getAnnotations())
//    }
//
//    private fun annotation(): Stmt.Annotation? {
//        var expr: Expr? = null
//        if (peek().type == LEFT_BRACKET) {
//            advance()
//            expr = objectLiteral()
//        } else if (peek().type == IDENTIFIER) {
//            expr = call()
//        } else {
//            Parser.error(peek(), "Annotation expect either an object literal or a constructor invocation!")
//        }
//        checkStatementEnd(false)
//        return Annotation(expr)
//    }

//    private fun forStatement(): Stmt? {
//        val forToken = previous()
//        var `var`: Token? = null
//        var decompExpr: Expr? = null
//        if (match(IDENTIFIER)) {
//            `var` = previous()
//        } else if (match(LEFT_BRACKET)) {
//            `var` = Token(IDENTIFIER, "var" + System.currentTimeMillis(), null, forToken.line, forToken.file)
//            decompExpr = objectLiteral()
//        } else {
//            Parser.error(peek(), "Expected identifier or object decomp in for loop.")
//        }
//        consume(IN, "Expected 'in'.")
//        val iterable: Expr = expression()
//        var prependedStmts: MutableList<Stmt?>? = null
//        val varExpr = Variable(`var`)
//        if (decompExpr != null) {
//            prependedStmts = ArrayList()
//            prependedStmts.add(Expression(Parser.getAssignExpr(this, decompExpr, forToken, varExpr)))
//        }
//        val body: Block = block("for", true, prependedStmts)
//        return For(varExpr, iterable, body)
//    }
//
//    private fun ifStmt(): Stmt? {
//        return ifSomething(IfProducer { ifstmt: Stmt.Elsif?, elsifs: List<Stmt.Elsif?>?, elseBranch: Block? -> Stmt.If(ifstmt, elsifs, elseBranch) }, ElsifProducer { condition: Expr?, thenBranch: Block? -> Stmt.Elsif(condition, thenBranch) })
//    }
//
//    private fun whenStmt(): Stmt? {
//        return whenSomething(IfProducer { ifstmt: Stmt.Elsif?, elsifs: List<Stmt.Elsif?>?, elseBranch: Block? -> Stmt.If(ifstmt, elsifs, elseBranch) }, ElsifProducer { condition: Expr?, thenBranch: Block? -> Stmt.Elsif(condition, thenBranch) })
//    }
//
//    private fun ifExpr(): Expr? {
//        return ifSomething(IfProducer { ifstmt: Expr.Elsif?, elsifs: List<Expr.Elsif?>?, elseBranch: Block? -> Expr.If(ifstmt, elsifs, elseBranch) }, ElsifProducer { condition: Expr?, thenBranch: Block? -> Expr.Elsif(condition, thenBranch) })
//    }
//
//    private fun whenExpr(): Expr? {
//        return whenSomething(IfProducer { ifstmt: Expr.Elsif?, elsifs: List<Expr.Elsif?>?, elseBranch: Block? -> Expr.If(ifstmt, elsifs, elseBranch) }, ElsifProducer { condition: Expr?, thenBranch: Block? -> Expr.Elsif(condition, thenBranch) })
//    }


    private fun ifSomething() {
        expression()
        val ifChunk = emitJump(OpCode.JUMP_IF_FALSE)
        emitCode(OpCode.POP)
        statement(true)
        val elseJump = emitJump(OpCode.JUMP)
        patchJump(ifChunk)
        emitCode(OpCode.POP)
        if (match(ELSE)) {
            statement(true)
        }
        patchJump(elseJump)
    }

//
//    private fun <T, E> ifSomething(ifProducer: IfProducer<T, E>, elsifProducer: ElsifProducer<E>): T {
//        val condition: Expr = expression()
//        val thenBranch: Block = block("if", true)
//        var elseBranch: Block? = null
//        val elsifs: MutableList<E> = ArrayList()
//        while (match(ELSIF, NEWLINE)) {
//            if (previous().type == ELSIF) {
//                elsifs.add(elsifProducer.go(expression(), block("elsif", true)))
//            }
//        }
//        if (match(ELSE)) {
//            elseBranch = block("else", true)
//        }
//        return ifProducer.go(elsifProducer.go(condition, thenBranch), elsifs, elseBranch)
//    }
//
//    private fun <T, E> whenSomething(ifProducer: IfProducer<T, E?>, elsifProducer: ElsifProducer<E>): T {
//        val `when` = previous()
//        val left: Expr = call()
//        consume(LEFT_BRACE, "Expect a '{' after when.")
//        consume(NEWLINE, "Expect a newline after when ':.")
//        var firstElsif: E? = null
//        val elsifs: MutableList<E?> = ArrayList()
//        var elseBranch: Block? = null
//        while (!check(RIGHT_BRACE) && !isAtEnd()) {
//            if (match(NEWLINE)) {
//                continue
//            }
//            val conditions: MutableList<Expr> = ArrayList()
//            do {
//                var op: Token
//                if (match(IS, ISNOT, IN, NOTIN, IF)) {
//                    op = previous()
//                } else if (match(ELSE)) {
//                    elseBranch = block(Parser.KIND_WHEN, true)
//                    break
//                } else {
//                    op = Token(EQUAL_EQUAL, null, null, `when`.line, `when`.file)
//                }
//                val right: Expr = call()
//                var condition: Expr
//                condition = if (op.type == IF) {
//                    right
//                } else {
//                    Binary(left, op, right)
//                }
//                conditions.add(condition)
//                match(OR)
//            } while (!check(LEFT_BRACE, COLON))
//            if (conditions.isEmpty()) {
//                continue
//            }
//            var condition = conditions[0]
//            val or = Token(OR, null, null, `when`.line, `when`.file)
//            for (i in 1 until conditions.size) {
//                condition = Logical(condition, or, conditions[i])
//            }
//            val elsif = elsifProducer.go(condition, block(Parser.KIND_WHEN, true))
//            if (firstElsif == null) {
//                firstElsif = elsif
//            } else {
//                elsifs.add(elsif)
//            }
//        }
//        consume(RIGHT_BRACE, "Expect } at the end of when.")
//        return ifProducer.go(firstElsif, elsifs, elseBranch)
//    }

    private fun printStatement(lambda: Boolean) {
//        val value: Expr = expressionStatement(lambda).expression
//        return Stmt.Print(value)
        expressionStatement(lambda)
        emitCode(OpCode.PRINT)
        sp--
    }

//    private fun returnStatement(lambda: Boolean): Stmt? {
//        val keyword = previous()
//        var value: Expr? = null
//        if (!check(NEWLINE)) {
//            value = expression()
//        }
//        checkStatementEnd(lambda)
//        return Return(keyword, value)
//    }
//
//    private fun yieldStatement(lambda: Boolean): Stmt? {
//        val keyword = previous()
//        var value: Expr? = null
//        if (!check(NEWLINE)) {
//            value = expression()
//        }
//        checkStatementEnd(lambda)
//        return Stmt.Yield(keyword, value)
//    }
//

//    private fun whileStatement(): Stmt? {
//        val condition: Expr = expression()
//        val block: Block = block("while", true)
//        return While(condition, block)
//    }
//
//    private fun breakStatement(): Stmt? {
//        val name = previous()
//        return Break(name)
//    }
//
//    private fun continueStatement(): Stmt? {
//        val name = previous()
//        return Continue(name)
//    }
//
//    private fun rescueStatement(): Stmt? {
//        val keyword = previous()
//        val block: Block = block("rescue", true)
//        if (block.params.size != 1) {
//            Parser.error(keyword, "Rescue block expects exactly 1 parameter!")
//        }
//        return Rescue(keyword, block)
//    }

    private fun expressionStatement(lambda: Boolean) {
       /* val expr: Expr = */expression()
//        if (!(expr is Assign && expr.value is Expr.Block)
//                && !(expr is Expr.Set && expr.value is Expr.Block)) {
//            // If the left-hand side is an assign or set and right-hand side is a block, then we've already consumed the
//            // line ending and don't have to check the lambda end.
//            checkStatementEnd(lambda)
//        }
//        return Stmt.Expression(expr)
    }

    private fun expression() {
        assignment()
    }

    private fun assignment() {
        or() // left-hand side
        if (match(EQUAL, DOLLAR_EQUAL, PLUS_EQUAL, MINUS_EQUAL, STAR_EQUAL, SLASH_EQUAL,
                        SLASH_SLASH_EQUAL, MOD_EQUAL, QUESTION_QUESTION_EQUAL)) {
            val equals = previous()
            if (equals.type == EQUAL) {
                if (lastChunk?.opCode != OpCode.CONST_ID) {
                    throw error(equals, "Expected an ID for var declaration!")
                }
                declareLocal(lastChunk!!.data[1] as String)
                rollBackLastChunk()
                or() // push the value on the stack
            } else {
                if (lastChunk?.opCode != OpCode.GET_LOCAL) {
                    throw error(equals, "Assigning to undeclared var!")
                }
                val local = lastChunk!!.data[0] as Local
                if (equals.type == DOLLAR_EQUAL) {
                    rollBackLastChunk() // It'll just be a set, set-assigns reuse the already emitted GET_LOCAL
                }
                or() // right-hand side
                if (equals.type != DOLLAR_EQUAL) {
                    emitCode(when (equals.type) {
                        PLUS_EQUAL -> OpCode.ADD
                        MINUS_EQUAL -> OpCode.SUBTRACT
                        STAR_EQUAL -> OpCode.MULTIPLY
                        SLASH_EQUAL -> OpCode.DIVIDE
                        SLASH_SLASH_EQUAL -> OpCode.DIVIDE_INT
                        MOD_EQUAL -> OpCode.MOD
                        else -> throw IllegalArgumentException("WTF")
                        // TODO extend
                    })
                }
                emitSetLocal(local)
            }
//            if (match(TokenType.YIELD)) {
//                val keyword = previous()
//                val call = call()
//                if (call is Call) {
//                    return Yield(expr, equals, keyword, call)
//                } else {
//                    throw error(keyword, "yield expressions must involve a call!")
//                }
//            }
            /*val value = assignment()*/
//            return Parser.getAssignExpr(this, expr, equals, value)
        }
//        return expr
    }

    private fun or() {
        var expr = and()
//        while (match(OR)) {
//            val operator = previous()
//            val right = and()
//            expr = Logical(expr, operator, right)
//        }
//        return expr
    }

    private fun and() {
        var expr = equality()
//        while (match(AND)) {
//            val operator = previous()
//            val right = equality()
//            expr = Logical(expr, operator, right)
//        }
//        return expr
    }

    private fun equality() {
        var expr = comparison()
//        while (match(BANG_EQUAL, EQUAL_EQUAL, IS, ISNOT, IN, NOTIN)) {
//            val operator = previous()
//            val right = comparison()
//            expr = Binary(expr, operator, right)
//        }
//        return expr
    }

    private fun comparison() {
        var expr = addition()
//        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL, LESS_GREATER)) {
//            val operator = previous()
//            val right = addition()
//            expr = Binary(expr, operator, right)
//        }
//        return expr
    }

    private fun addition() {
        var expr = multiplication()
        while (match(MINUS, PLUS)) {
            val operator = previous()
            multiplication()
            emitCode(if (operator.type == PLUS) OpCode.ADD else OpCode.SUBTRACT)
            sp--
//            val right = multiplication()
//            expr = Binary(expr, operator, right)
        }
//        return expr
    }

    private fun multiplication() {
        var expr = nilCoalescence()
        while (match(SLASH, SLASH_SLASH, STAR, MOD)) {
            val operator = previous()
            nilCoalescence()
            emitCode(when (operator.type) {
                SLASH -> OpCode.DIVIDE
                SLASH_SLASH -> OpCode.DIVIDE_INT
                STAR -> OpCode.MULTIPLY
                MOD -> OpCode.MOD
                else -> throw IllegalArgumentException("WTF")
            })
            sp--
//            val right = nilCoalescence()
//            expr = Expr.Binary(expr, operator, right)
        }
//        return expr
    }

    private fun nilCoalescence() {
        var expr = unary()
//        while (match(QUESTION_QUESTION)) {
//            val operator = previous()
//            val right = unary()
//            expr = Binary(expr, operator, right)
//        }
//        return expr
    }

    private fun unary() {
        if (match(NOT, MINUS, BANG_BANG)) {
//            val operator = previous()
//            val right = unary()
            unary()
            emitCode(OpCode.NEGATE)
//            return Unary(operator, right)
        }
//        if (match(GU)) {
//            return Gu(unary())
//        }
        /*return if (match(IVIC)) {
            Ivic(unary())
        } else*/ call()
    }

    private fun call() {
        var expr = primary()
//        while (true) {
//            if (match(TokenType.LEFT_PAREN, TokenType.DOLLAR_LEFT_PAREN)) {
//                expr = finishCall(expr)
//            } else if (match(TokenType.DOT)) {
//                val dot = previous()
//                var name: Expr
//                if (peek().type == TokenType.NUMBER) {
//                    name = Variable(consume(TokenType.NUMBER, "Expected a number or id after '.'."))
//                } else if (peek().type == TokenType.LEFT_PAREN) {
//                    name = primary()
//                } else if (peek().type == TokenType.CLASS) {
//                    name = Literal(SimiValue.String(Constants.CLASS))
//                    advance()
//                } else {
//                    name = Variable(consume(TokenType.IDENTIFIER, "Expected a number of id after '.'."))
//                }
//                val arity: Int = peekParams()
//                expr = Get(dot, expr, name, arity)
//            } else {
//                break
//            }
//        }
//        return expr
    }

    private fun finishCall(callee: Expr) {
//        val paren = previous()
//        val arguments: MutableList<Expr> = ArrayList()
//        if (!check(TokenType.RIGHT_PAREN)) {
//            do {
//                matchSequence(TokenType.IDENTIFIER, TokenType.EQUAL) // allows for named params, e.g substr(start=1,end=2)
//                arguments.add(expression())
//            } while (match(TokenType.COMMA))
//        }
//        consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments.")
//        if (callee is Variable && callee.backupSelfGet != null) {
//            // If a variable callee has a backup get, its arity needs to be adjusted because we're dealing with a call
//            // and not a single variable load.
//            callee.backupSelfGet.arity = arguments.size
//        }
//        return Call(callee, paren, arguments)
    }

    private fun primary() {
        if (match(FALSE)) {
            emitInt(0)
        }
        else if (match(TRUE)) {
            emitInt(1)
        }
        else if (match(NIL)) {
            emitCode(OpCode.NIL)
        }
//        if (match(TokenType.PASS)) {
//            return Literal(Pass())
//        }
//        if (match(TokenType.NATIVE)) {
//            return Literal(Native())
//        }
        else if (match(NUMBER, STRING)) {
            val token = previous()
            when (token.literal) {
                is SimiValue.Number -> {
                    if (token.literal.number.isInteger) {
                        emitInt(token.literal.number.asLong())
                    } else {
                        emitFloat(token.literal.number.asDouble())
                    }
                }
                is SimiValue.String -> emitString(token.literal.string.toString())
            }
//            return Literal(previous()!!.literal)
        }
//        if (match(TokenType.SUPER)) {
//            val keyword = previous()
//            val superclass: Token?
//            if (match(TokenType.LEFT_PAREN)) {
//                superclass = consume(TokenType.IDENTIFIER, "Expected superclass name in parentheses!")
//                consume(TokenType.RIGHT_PAREN, "Expected ')' after superclass specification!")
//            } else {
//                superclass = null
//            }
//            consume(TokenType.DOT, "Expect '.' after 'super'.")
//            val method = consume(TokenType.IDENTIFIER,
//                    "Expect superclass method name.")
//            val arity: Int = peekParams()
//            return Super(keyword, superclass, method, arity)
//        }
//        if (match(TokenType.SELF)) {
//            val previous = previous()
//            var specifier: Token? = null
//            if (peekSequence(TokenType.LEFT_PAREN, TokenType.DEF, TokenType.RIGHT_PAREN)) {
//                specifier = Token(TokenType.DEF, Constants.SELF_DEF, null, previous!!.line, previous.file)
//                advance()
//                advance()
//                advance()
//            }
//            return Self(Token(TokenType.SELF, Constants.SELF, null, previous!!.line, previous.file), specifier)
//        }
//        if (match(TokenType.LEFT_BRACKET, TokenType.DOLLAR_LEFT_BRACKET)) {
//            return objectLiteral()
//        }
//        if (match(TokenType.DEF)) {
//            return block(Parser.KIND_LAMBDA, true)
//        }
//        if (match(TokenType.EQUAL)) {
//            return shorthandBlock(previous())
//        }
        else if (match(IDENTIFIER)) {
            val id = previous()
            val name = id.lexeme
            val local = findLocal(name)
            if (local != null) {
                emitGetLocal(local)
            } else {
                emitId(name)
            }
//            val expr = Variable(previous())
//            if (isInClassDeclr) { // Add backup self.var
//                val selfToken = Token.self()
//                expr.backupSelfGet = Get(selfToken, Self(selfToken, null), expr, 0)
//            }
//            return expr
        }
        else if (match(LEFT_PAREN)) {
            expression()
            consume(RIGHT_PAREN, "Expect ')' after expression.")
        }
//        if (match(TokenType.QUESTION)) {
//            return Unary(previous(), primary())
//        }
//        if (match(TokenType.IF)) {
//            return ifExpr()
//        }
//        if (match(TokenType.WHEN)) {
//            return whenExpr()
//        }
        else {
            throw error(peek(), "Expect expression.")
        }
    }

//    private fun objectLiteral(): Expr {
//        val opener = previous()
//        val props: MutableList<Expr> = ArrayList()
//        if (!check(TokenType.RIGHT_BRACKET)) {
//            matchAllNewlines()
//            do {
//                matchAllNewlines()
//                props.add(assignment())
//                matchAllNewlines()
//            } while (match(TokenType.COMMA))
//            matchAllNewlines()
//        }
//        consume(TokenType.RIGHT_BRACKET, "Expect ']' at the end of object.")
//        return ObjectLiteral(opener, props)
//    }

    private fun checkStatementEnd(lambda: Boolean) {
        if (match(NEWLINE, RIGHT_BRACE, EOF)) {
            return
        }
        if (lambda) {
            val token = peek()
            if (token.type == COMMA || token.type == RIGHT_PAREN || token.type == RIGHT_BRACKET) {
                return
            }
        }
        throw error(peek(), "Unterminated lambda expression!")
    }

    private fun matchAllNewlines() {
        while (match(NEWLINE)) {
        }
    }

    private fun matchSequence(vararg types: TokenType): Boolean {
        for (i in types.indices) {
            val index: Int = current + i
            if (index >= tokens.size) {
                return false
            }
            if (tokens[index].type != types[i]) {
                return false
            }
        }
        for (i in types.indices) {
            advance()
        }
        return true
    }

    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }

    private fun consume(type: TokenType, message: String): Token? {
        if (check(type)) return advance()
        throw error(peek(), message)
    }

    private fun check(vararg tokenTypes: TokenType): Boolean {
        for (tokenType in tokenTypes) {
            if (isAtEnd() && tokenType == EOF) {
                return true
            }
            if (peek().type == tokenType) {
                return true
            }
        }
        return false
    }

    private fun advance(): Token? {
        if (!isAtEnd()) current++
        return previous()
    }

    private fun isAtEnd(): Boolean {
        return peek().type == EOF
    }

    private fun peek(): Token {
        return tokens[current]
    }

    private fun peekSequence(vararg tokenTypes: TokenType): Boolean {
        if (current + tokenTypes.size >= tokens.size) {
            return false
        }
        for (i in tokenTypes.indices) {
            if (tokens[current + i].type != tokenTypes[i]) {
                return false
            }
        }
        return true
    }

    private fun previous(): Token {
        return tokens[current - 1]
    }

    private fun rollBack(by: Int = 1) {
        for (i in 0 until by) {
            byteCode.removeAt(byteCode.size - 1)
        }
    }

    private fun rollBackLastChunk() {
        rollBack(lastChunk!!.size)
        chunks.pop()
        lastChunk = chunks.last
    }

    private fun error(token: Token, message: String): ParseError {
//        ErrorHub.sharedInstance().error(Constants.EXCEPTION_PARSER, token, message)
        return ParseError("At $token: $message")
    }

    private fun strIndex(s: String): Int {
        return strTable[s] ?: run {
            strTable[s] = strCount++
            strList += s
            strCount - 1
        }
    }

    private fun emitCode(opCode: OpCode) {
        val size = byteCode.put(opCode)
        pushLastChunk(Chunk(opCode, size))
    }

    private fun emitInt(i: Long) {
        val opCode = OpCode.CONST_INT
        var size = byteCode.put(opCode)
        size += byteCode.putLong(i)
        pushLastChunk(Chunk(opCode, size, i))
        sp++
    }

    private fun emitFloat(f: Double) {
        val opCode = OpCode.CONST_FLOAT
        var size = byteCode.put(opCode)
        size += byteCode.putDouble(f)
        pushLastChunk(Chunk(opCode, size, f))
        sp++
    }

    private fun emitId(id: String) {
        emitIdOrString(OpCode.CONST_ID, id)
    }

    private fun emitString(string: String) {
        emitIdOrString(OpCode.CONST_STR, string)
    }

    private fun emitIdOrString(opCode: OpCode, s: String) {
        var size = byteCode.put(opCode)
        val strIdx = strIndex(s)
        size += byteCode.putInt(strIdx)
        pushLastChunk(Chunk(opCode, size, strIdx, s))
    }

    private fun emitGetLocal(local: Local) {
        val opCode = OpCode.GET_LOCAL
        var size = byteCode.put(opCode)
        size += byteCode.putInt(local.sp)
        pushLastChunk(Chunk(opCode, size, local))
        sp++
    }

    private fun emitSetLocal(local: Local) {
        val opCode = OpCode.SET_LOCAL
        var size = byteCode.put(opCode)
        size += byteCode.putInt(local.sp)
        pushLastChunk(Chunk(opCode, size, local))
    }

    private fun emitJump(opCode: OpCode): Chunk {
        var size = byteCode.put(opCode)
        val offset = byteCode.size
        size += byteCode.putInt(0)
        val chunk = Chunk(opCode, size, offset)
        pushLastChunk(chunk)
        return chunk

//        size += byteCode.emitMarkingPosition {
//            emitCode(OpCode.POP)
//            statement(true)
//        }
//        chunk.size = size
    }

    private fun patchJump(chunk: Chunk) {
        val offset = chunk.data[0] as Int
        val skip = byteCode.size
        byteCode.setInt(skip, offset)
        chunk.size += skip - offset
    }

    private fun pushLastChunk(chunk: Chunk) {
        lastChunk = chunk
        chunks.push(chunk)
    }

    private fun declareLocal(name: String): Local {
        for (i in locals.size - 1 downTo 0) {
            val local = locals[i]
            if (local.depth < scopeDepth) {
                break
            }
            if (local.name == name) {
                throw error(previous(), "Variable $name is already declared in scope!")
            }
        }
        val l = Local(name, sp, scopeDepth)
        locals += l
        return l
    }

    private fun findLocal(name: String): Local? {
        for (i in locals.size - 1 downTo 0) {
            val local = locals[i]
            if (local.name == name) {
                return local
            }
        }
        return null
    }

    private fun printChunks() {
        while (chunks.isNotEmpty()) {
            println(chunks.last)
            chunks.removeLast()
        }
    }

    private data class Local(val name: String, val sp: Int, val depth: Int)

    private class Chunk(val opCode: OpCode,
                        var size: Int,
                        vararg val data: Any) {
        override fun toString(): String {
            return "$opCode, size: $size b, data: ${data.joinToString()}"
        }
    }

    private class ParseError(message: String) : RuntimeException(message)

    enum class OpCode {
        CONST_INT,
        CONST_FLOAT,
        CONST_ID,
        CONST_STR,
        NIL,
        POP,
        PUSH_SCOPE,
        POP_SCOPE,
        SET_LOCAL,
        GET_LOCAL,
        NEGATE,
        ADD,
        SUBTRACT,
        MULTIPLY,
        DIVIDE,
        DIVIDE_INT,
        MOD,
        PRINT,
        JUMP,
        JUMP_IF_FALSE,
        HALT,
        ;

        val byte = ordinal.toByte()

        companion object {
            fun from(byte: Byte) = values()[byte.toInt()]
        }
    }

    class CompilerOutput(val byteCode: ByteArray, val strings: List<String>)
}