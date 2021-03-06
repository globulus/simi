package net.globulus.simi.warp

import net.globulus.simi.Token
import net.globulus.simi.TokenType
import net.globulus.simi.TokenType.*
import net.globulus.simi.TokenType.MOD
import net.globulus.simi.TokenType.NIL
import net.globulus.simi.TokenType.PRINT
import net.globulus.simi.api.SimiValue
import net.globulus.simi.warp.Kompiler.OpCode.*
import java.util.*

internal class Kompiler {
    private lateinit var tokens: List<Token>
    private var current = 0

    private val locals = mutableListOf<Local>()
    private var scopeDepth = -1 // Will be set to 0 with first beginScope()
    private val loops = Stack<ActiveLoop>()

    private var lastChunk: Chunk? = null
    private val chunks: Deque<Chunk> = LinkedList()

    fun compile(tokens: List<Token>): String {
        this.tokens = tokens
        current = 0
        beginScope()
        compileInternal()
        endScope()
        emitCode(OpCode.HALT)
//        printChunks()
        val sb = StringBuilder()
        for (chunk in chunks.reversed()) {
            sb.appendln(when (chunk.opCode) {
                OpCode.CONST_INT -> "push(${chunk.get<Long>(0)}L)"
                OpCode.CONST_FLOAT -> "push(${chunk.get<Double>(0)})"
                OpCode.CONST_ID -> throw RuntimeException("WTF")
                OpCode.CONST_STR -> "push(\"${chunk.get<String>(0).replace("\"", "\\\"")}\")"
                OpCode.NIL -> "push(Nil)"
                OpCode.POP -> "sp--"
                OpCode.SET_LOCAL -> "stack[${chunk.get<Local>(0).sp}] = pop()"
                OpCode.GET_LOCAL -> "push(stack[${chunk.get<Local>(0).sp}]!!)"
                OpCode.NEGATE -> "negate()"
                OpCode.ADD -> "add()"
                OpCode.SUBTRACT, OpCode.MULTIPLY, OpCode.DIVIDE, OpCode.DIVIDE_INT, OpCode.MOD, OpCode.LE, OpCode.LT, OpCode.GE, OpCode.GT -> "binaryOpOnStack(OpCode.${chunk.opCode})"
                OpCode.EQ, OpCode.NE -> "checkEquality(OpCode.${chunk.opCode})"
                OpCode.PRINT -> "println(pop())"
                OpCode.IF_TRUE -> "if (!isFalsey(peek())) {"
                OpCode.IF_FALSE -> "if (isFalsey(peek())) {"
                OpCode.ELSEOP -> " else {"
                WHILEOP -> "while(true) {"
                BREAKOP -> "break"
                CONTINUEOP -> "continue"
                OpCode.END -> "}"
//                OpCode.JUMP -> buffer.position(nextInt)
//                OpCode.JUMP_IF_FALSE -> {
//                    val offset = nextInt
//                    if (isFalsey(peek())) {
//                        buffer.position(offset)
//                    }
//                }
//                OpCode.HALT -> break@loop
                else -> ""
            })
        }
        return sb.toString()
    }

    /**
     * Moved to a separate fun to be able to handle syntax sugar compilations such as when.
     */
    private fun compileInternal() {
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
    }

    private fun compileNested(tokens: List<Token>) {
        val currentSave = current
        current = 0
        val tokensSave = this.tokens
        this.tokens = tokens + Token.copying(tokens.last(), EOF)
        compileInternal()
        this.tokens = tokensSave
        current = currentSave
    }

    private fun beginScope() {
        scopeDepth++
    }

    private fun endScope() {
        discardLocals(scopeDepth)
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
            ifSomething()
        }
        else if (match(WHEN)) {
            whenSomething()
        }
//        if (match(TokenType.FOR)) {
//            return forStatement()
//        }
        else if (match(PRINT)) {
            printStatement()
        }
//        if (match(TokenType.RETURN)) {
//            return returnStatement(lambda)
//        }
        else if (match(WHILE)) {
            whileStatement()
        }
        else if (match(BREAK)) {
            breakStatement()
        }
        else if (match(CONTINUE)) {
            continueStatement()
        }
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

    private fun ifSomething() {
        expression()
        emitCode(OpCode.IF_TRUE)
        emitCode(POP)
        statement(true)
        emitCode(END)
        emitCode(OpCode.ELSEOP)
        emitCode(POP)
        if (match(TokenType.ELSE)) {
            statement(true)
        }
        emitCode(END)
    }

    /**
     * Actually creates a list of tokens that represent if-else-ifs that are then compiled internally
     */
    private fun whenSomething() {
        val origin = previous()
        val id = consume(IDENTIFIER, "Expected an identifier after 'when'")
        var first = true
        var wroteElse = false
        val whenTokens = mutableListOf<Token>()
        consume(LEFT_BRACE, "Expect a '{' after when")
        consume(NEWLINE, "Expect a newline after when '{'")
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            matchAllNewlines()
            if (match(ELSE)) {
                wroteElse = true
                whenTokens += previous()
                whenTokens += consumeNextBlock()
            } else if (wroteElse) {
                // We could just break when we encountered a break, but that's make for a lousy compiler
                throw error(previous(), "'else' must be the last clause in a 'when' block")
            } else {
                var ifToken: Token? = null
                do {
                    val op = if (match(IS, ISNOT, IN, NOTIN, IF)) {
                        previous()
                    } else {
                        Token.copying(origin, EQUAL_EQUAL)
                    }

                    // Emit the beginning of the statement if it hasn't been already
                    if (ifToken == null) {
                        ifToken = Token.copying(op, IF)
                        if (first) {
                            first = false
                        } else {
                            whenTokens += Token.copying(ifToken, ELSE)
                        }
                        whenTokens += ifToken!!
                    }

                    // If the op type is IF, we'll just evaluate what comes after it,
                    // otherwise it's a check against the id.
                    if (op.type != IF) {
                        whenTokens += id
                        whenTokens += op
                    }

                    // Consume the rest of the condition
                    whenTokens += consumeUntilType(OR, LEFT_BRACE)

                    // If we've found an or, we just take it as-is
                    if (match(OR)) {
                        whenTokens += previous()
                    }
                } while (!check(LEFT_BRACE))

                // Consume the statement
                whenTokens += consumeNextBlock()
            }
        }
        consume(RIGHT_BRACE, "Expect '}' at the end of when")
        compileNested(whenTokens)
    }

    private fun printStatement() {
        expression()
        emitCode(OpCode.PRINT)
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

    private fun whileStatement() {
        loops.push(ActiveLoop(0, scopeDepth))
        emitCode(WHILEOP)
        expression()
        emitCode(IF_TRUE)
        emitCode(POP)
        statement(true)
        emitCode(END)
        emitCode(ELSEOP)
        emitCode(POP)
        emitCode(BREAKOP)
        emitCode(END)
        emitCode(END)
        loops.pop()
    }

    private fun breakStatement() {
        if (loops.isEmpty()) {
            throw error(previous(), "Cannot 'break' outside of a loop!")
        }
        val activeLoop = loops.peek()
        // Discards scopes up to the loop level (hence + 1)
        for (depth in scopeDepth downTo (activeLoop.depth + 1)) {
            discardLocals(depth)
        }
        emitCode(BREAKOP)
    }

    private fun continueStatement() {
        if (loops.isEmpty()) {
            throw error(previous(), "Cannot 'continue' outside of a loop!")
        }
        val activeLoop = loops.peek()
        // Discards scopes up to the level of loop statement (hence + 2)
        for (depth in scopeDepth downTo (activeLoop.depth + 2)) {
            discardLocals(depth)
        }
        emitCode(CONTINUEOP)
    }

//    private fun rescueStatement(): Stmt? {
//        val keyword = previous()
//        val block: Block = block("rescue", true)
//        if (block.params.size != 1) {
//            Parser.error(keyword, "Rescue block expects exactly 1 parameter!")
//        }
//        return Rescue(keyword, block)
//    }

    private fun expressionStatement(lambda: Boolean) {
        val shouldPop = expression()
        if (shouldPop) {
            emitCode(POP)
        }
//        if (!(expr is Assign && expr.value is Expr.Block)
//                && !(expr is Expr.Set && expr.value is Expr.Block)) {
//            // If the left-hand side is an assign or set and right-hand side is a block, then we've already consumed the
//            // line ending and don't have to check the lambda end.
//            checkStatementEnd(lambda)
//        }
//        return Stmt.Expression(expr)
    }

    private fun expression(): Boolean {
        return assignment()
    }

    private fun assignment(): Boolean {
        or() // left-hand side
        if (match(EQUAL, DOLLAR_EQUAL, PLUS_EQUAL, MINUS_EQUAL, STAR_EQUAL, SLASH_EQUAL,
                        SLASH_SLASH_EQUAL, MOD_EQUAL, QUESTION_QUESTION_EQUAL)) {
            val equals = previous()
            if (equals.type == EQUAL) {
                if (lastChunk?.opCode != CONST_ID) {
                    throw error(equals, "Expected an ID for var declaration!")
                }
                declareLocal(lastChunk!!.data[0] as String)
                rollBackLastChunk()
                or() // push the value on the stack
            } else {
                if (lastChunk?.opCode != GET_LOCAL) {
                    throw error(equals, "Assigning to undeclared var!")
                }
                val local = lastChunk!!.data[0] as Local
                if (equals.type == DOLLAR_EQUAL) {
                    rollBackLastChunk() // It'll just be a set, set-assigns reuse the already emitted GET_LOCAL
                }
                or() // right-hand side
                if (equals.type != DOLLAR_EQUAL) {
                    emitCode(when (equals.type) {
                        PLUS_EQUAL -> ADD
                        MINUS_EQUAL -> SUBTRACT
                        STAR_EQUAL -> MULTIPLY
                        SLASH_EQUAL -> DIVIDE
                        SLASH_SLASH_EQUAL -> DIVIDE_INT
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
            return false
        }
        return true
//        return expr
    }

    private fun or() {
        and()
        while (match(OR)) {
            emitCode(IF_FALSE)
            emitCode(POP)
            and()
            emitCode(END)
//            val elseChunk = emitJump(JUMP_IF_FALSE)
//            val endChunk = emitJump(JUMP)
//            patchJump(elseChunk)
//            emitCode(POP)
//            and()
//            patchJump(endChunk)
        }
    }

    private fun and() {
        equality()
        while (match(AND)) {
            emitCode(IF_TRUE)
            emitCode(POP)
            equality()
            emitCode(END)
        }
    }

    private fun equality() {
        comparison()
        while (match(BANG_EQUAL, EQUAL_EQUAL/*, IS, ISNOT, IN, NOTIN*/)) {
            val operator = previous()
            comparison()
            emitCode(when (operator.type) {
                EQUAL_EQUAL -> EQ
                BANG_EQUAL -> NE
                else -> throw IllegalArgumentException("WTF")
            })
        }
    }

    private fun comparison() {
        addition()
        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL/*, LESS_GREATER*/)) {
            val operator = previous()
            addition()
            emitCode(when (operator.type) {
                GREATER -> GT
                GREATER_EQUAL -> GE
                LESS -> LT
                LESS_EQUAL -> LE
                else -> throw IllegalArgumentException("WTF")
            })
        }
    }

    private fun addition() {
        multiplication()
        while (match(MINUS, PLUS)) {
            val operator = previous()
            multiplication()
            emitCode(if (operator.type == PLUS) ADD else SUBTRACT)
        }
    }

    private fun multiplication() {
        nilCoalescence()
        while (match(SLASH, SLASH_SLASH, STAR, MOD)) {
            val operator = previous()
            nilCoalescence()
            emitCode(when (operator.type) {
                SLASH -> DIVIDE
                SLASH_SLASH -> DIVIDE_INT
                STAR -> MULTIPLY
                MOD -> OpCode.MOD
                else -> throw IllegalArgumentException("WTF")
            })
        }
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
            val operator = previous()
//            val right = unary()
            unary()
            emitCode(NEGATE)
        }
//        if (match(GU)) {
//            return Gu(unary())
//        }
        /*return if (match(IVIC)) {
            Ivic(unary())
        } */
        else {
            call()
        }
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

    private fun consumeNextBlock(): List<Token> {
        val start = current
        var braceCount = 0
        var end = 0
        loop@ for (i in start until tokens.size) {
            when (tokens[i].type) {
                LEFT_BRACE -> braceCount++
                RIGHT_BRACE -> {
                    braceCount--
                    if (braceCount == 0) {
                        end = i
                        break@loop
                    }
                }
            }
        }
        current = end + 1
        val sublist = tokens.subList(start, current)
        matchAllNewlines()
        return sublist
    }

    private fun consumeUntilType(vararg types: TokenType): List<Token> {
        val start = current
        while (!check(*types)) {
            current++
        }
        return tokens.subList(start, current)
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

    private fun consume(type: TokenType, message: String): Token {
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

    private fun advance(): Token {
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

    private fun rollBackLastChunk() {
        chunks.pop()
        lastChunk = if (chunks.isEmpty()) null else chunks.last
    }

    private fun error(token: Token, message: String): ParseError {
//        ErrorHub.sharedInstance().error(Constants.EXCEPTION_PARSER, token, message)
        return ParseError("At $token: $message")
    }
    
    private fun emitCode(opCode: OpCode) {
        pushLastChunk(Chunk(opCode))
    }

    private fun emitInt(i: Long) {
        val opCode = CONST_INT
        pushLastChunk(Chunk(opCode, i))
    }

    private fun emitFloat(f: Double) {
        val opCode = CONST_FLOAT
        pushLastChunk(Chunk(opCode, f))
    }

    private fun emitId(id: String) {
        emitIdOrString(CONST_ID, id)
    }

    private fun emitString(string: String) {
        emitIdOrString(CONST_STR, string)
    }

    private fun emitIdOrString(opCode: OpCode, s: String) {
        pushLastChunk(Chunk(opCode, s))
    }

    private fun emitGetLocal(local: Local) {
        val opCode = GET_LOCAL
        pushLastChunk(Chunk(opCode, local))
    }

    private fun emitSetLocal(local: Local) {
        val opCode = SET_LOCAL
        pushLastChunk(Chunk(opCode, local))
    }

//    private fun emitJump(opCode: OpCode, location: Int? = null): Chunk {
//        var size = byteCode.put(opCode)
//        val offset = byteCode.size
//        size += byteCode.putInt(location ?: 0)
//        val chunk = Chunk(opCode, size, offset)
//        pushLastChunk(chunk)
//        return chunk
//
////        size += byteCode.emitMarkingPosition {
////            emitCode(OpCode.POP)
////            statement(true)
////        }
////        chunk.size = size
//    }
//
//    private fun patchJump(chunk: Chunk): Int {
//        val offset = chunk.data[0] as Int
//        val skip = byteCode.size
//        byteCode.setInt(skip, offset)
//        chunk.size += skip - offset
//        return skip
//    }

    private fun pushLastChunk(chunk: Chunk) {
        lastChunk = chunk
        chunks.push(chunk)
    }

    private fun declareLocal(name: String): Local {
        val end = locals.size
        for (i in end - 1 downTo 0) {
            val local = locals[i]
            if (local.depth < scopeDepth) {
                break
            }
            if (local.name == name) {
                throw error(previous(), "Variable $name is already declared in scope!")
            }
        }
        val l = Local(name, end, scopeDepth)
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

    private fun discardLocals(depth: Int) {
        var i = locals.size - 1
        while (i >= 0 && locals[i].depth == depth) {
            locals.removeAt(i)
            i--
            emitCode(POP)
        }
    }

    private fun printChunks() {
        while (chunks.isNotEmpty()) {
            println(chunks.last)
            chunks.removeLast()
        }
    }

    private data class Local(val name: String, val sp: Int, val depth: Int)

    private data class ActiveLoop(val start: Int, val depth: Int) {
        val breakPositions = mutableListOf<Int>()
    }

    private class Chunk(val opCode: OpCode,
                        vararg val data: Any) {

        operator fun <T> get(index: Int) = data[index] as T

        override fun toString(): String {
            return "$opCode, data: ${data.joinToString()}"
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
        SET_LOCAL,
        GET_LOCAL,
        LT,
        LE,
        GT,
        GE,
        EQ,
        NE,
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
        IF_TRUE,
        IF_FALSE,
        ELSEOP,
        WHILEOP,
        BREAKOP,
        CONTINUEOP,
        END,
        HALT,
        ;

        val byte = ordinal.toByte()

        companion object {
            fun from(byte: Byte) = values()[byte.toInt()]
        }
    }

    class CompilerOutput(val byteCode: ByteArray, val strings: List<String>)
}