package net.globulus.simi.warp

import net.globulus.simi.Parser
import net.globulus.simi.Token
import net.globulus.simi.TokenType
import net.globulus.simi.TokenType.*
import net.globulus.simi.TokenType.MOD
import net.globulus.simi.TokenType.NIL
import net.globulus.simi.TokenType.PRINT
import net.globulus.simi.api.SimiValue
import net.globulus.simi.tool.*
import net.globulus.simi.warp.OpCode.*
import java.util.*

class Compiler {
    private lateinit var byteCode: MutableList<Byte>

    private lateinit var tokens: List<Token>
    private var current = 0

    private val constTable = mutableMapOf<Any, Int>()
    private val constList = mutableListOf<Any>()
    private var constCount = 0

    private val functionTable = mutableMapOf<String, Int>() // maps function signatures to their const table indices

    private val locals = mutableListOf<Local>()
    private var scopeDepth = -1 // Will be set to 0 with first beginScope()
    private val loops = Stack<ActiveLoop>()

    private var lastChunk: Chunk? = null
    private val chunks: Deque<Chunk> = LinkedList()

    fun compile(tokens: List<Token>) = compile(tokens, SCRIPT, 0) {
        compileInternal()
        emitReturn()
    }

    private fun compile(tokens: List<Token>, name: String, arity: Int, within: Compiler.() -> Unit): Function {
        byteCode = mutableListOf()
        this.tokens = tokens
        current = 0
        locals += Local("", 0, 0) // Stores the top-level function
        beginScope()
        this.within()
        endScope()
        if (name == SCRIPT) {
            printChunks()
        }
        return Function(name, arity, byteCode.toByteArray(), constList.toTypedArray())
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
        if (match(DEF)) {
            function(Parser.KIND_FUNCTION)
        }
//        if (match(TokenType.BANG)) {
//            return annotation()
//        }
//        if (match(TokenType.IMPORT)) {
//            val keyword = previous()
//            if (match(TokenType.IDENTIFIER)) {
//                return Stmt.Import(keyword, Expr.Variable(previous()))
//            }
//        }
        else {
            statement(false)
        }
    }

    private fun function(kind: String) {
        val declaration = previous()
        val name = consumeVar("Expected an identifier for function name")
        val args = mutableListOf<String>()
        if (match(LEFT_PAREN)) {
            if (!check(RIGHT_PAREN)) {
                do {
                    val paramName = consumeVar("Expected param name")
                    args += paramName
                } while (match(COMMA))
            }
            consume(RIGHT_PAREN, "Expected )")
        }

        consume(LEFT_BRACE, "Expected {")
        val curr = current
        val funcCompiler = Compiler()
        val f = funcCompiler.compile(tokens, name, args.size) {
            current = curr
            args.forEach { declareLocal(it) }
            block()
            emitReturn()
        }
        current = funcCompiler.current
        functionTable[name] = constIndex(f)
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
        val ifChunk = emitJump(JUMP_IF_FALSE)
        emitCode(POP)
        statement(true)
        val elseJump = emitJump(JUMP)
        patchJump(ifChunk)
        emitCode(POP)
        if (match(ELSE)) {
            statement(true)
        }
        patchJump(elseJump)
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
        val start = byteCode.size
        loops.push(ActiveLoop(start, scopeDepth))
        expression()
        val skipChunk = emitJump(JUMP_IF_FALSE)
        emitCode(POP)
        statement(true)
        emitJump(JUMP, start)
        val end = patchJump(skipChunk)
        val breaksToPatch = loops.pop().breakPositions
        for (pos in breaksToPatch) {
            // Set to jump 1 after end to skip the final POP as it already happened in the loop body
            byteCode.setInt(end + 1, pos)
        }
        emitCode(POP)
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
        val chunk = emitJump(JUMP)
        activeLoop.breakPositions += chunk.data[0] as Int
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
        emitJump(JUMP, loops.peek().start)
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
                declareLocal(lastChunk!!.data[1] as String)
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
            val elseChunk = emitJump(JUMP_IF_FALSE)
            val endChunk = emitJump(JUMP)
            patchJump(elseChunk)
            emitCode(POP)
            and()
            patchJump(endChunk)
        }
    }

    private fun and() {
        equality()
        while (match(AND)) {
            val endChunk = emitJump(JUMP_IF_FALSE)
            emitCode(POP)
            equality()
            patchJump(endChunk)
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
        while (true) {
            if (match(LEFT_PAREN, DOLLAR_LEFT_PAREN)) {
                expr = finishCall()
//            } else if (match(TokenType.DOT)) {
//                val dot = previous()
//                var name: Expr
//                if (peek().type == TokenType.NUMBER) {
//                    name = Variable(consume(NUMBER, "Expected a number or id after '.'."))
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
            } else {
                break
            }
        }
//        return expr
    }

    private fun finishCall() {
        val paren = previous()
        if (lastChunk?.opCode != CONST_ID) {
            throw error(paren, "Expected an ID for call!")
        }
        val idx = functionTable[lastChunk!!.data[1] as String]!!
        rollBackLastChunk()
        emitConstWithIndex(idx)
        val argCount = argList()
        emitCall(argCount)
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

    private fun argList(): Int {
        var count = 0
        if (!check(RIGHT_PAREN)) {
            do {
                count++
                expression()
            } while (match(COMMA))
        }
        consume(RIGHT_PAREN, "Expect ')' after arguments.")
        return count
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
                is SimiValue.String -> emitConst(token.literal.string.toString())
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

    private fun consumeVar(message: String): String {
        return consume(IDENTIFIER, message).lexeme
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

    private fun rollBack(by: Int = 1) {
        for (i in 0 until by) {
            byteCode.removeAt(byteCode.size - 1)
        }
    }

    private fun rollBackLastChunk() {
        rollBack(lastChunk!!.size)
        chunks.pop()
        lastChunk = if (chunks.isEmpty()) null else chunks.last
    }

    private fun error(token: Token, message: String): ParseError {
//        ErrorHub.sharedInstance().error(Constants.EXCEPTION_PARSER, token, message)
        return ParseError("At $token: $message")
    }

    private fun constIndex(c: Any): Int {
        return constTable[c] ?: run {
            constTable[c] = constCount++
            constList += c
            constCount - 1
        }
    }

    private fun emitCode(opCode: OpCode) {
        val size = byteCode.put(opCode)
        pushLastChunk(Chunk(opCode, size))
    }

    private fun emitInt(i: Long) {
        val opCode = CONST_INT
        var size = byteCode.put(opCode)
        size += byteCode.putLong(i)
        pushLastChunk(Chunk(opCode, size, i))
    }

    private fun emitFloat(f: Double) {
        val opCode = CONST_FLOAT
        var size = byteCode.put(opCode)
        size += byteCode.putDouble(f)
        pushLastChunk(Chunk(opCode, size, f))
    }

    private fun emitId(id: String) {
        emitIdOrConst(CONST_ID, id)
    }

    private fun emitConst(c: Any) {
        emitIdOrConst(CONST, c)
    }

    private fun emitConstWithIndex(idx: Int) {
        var size = byteCode.put(CONST)
        size += byteCode.putInt(idx)
        pushLastChunk(Chunk(CONST, size, idx))
    }

    private fun emitIdOrConst(opCode: OpCode, c: Any) {
        var size = byteCode.put(opCode)
        val constIdx = constIndex(c)
        size += byteCode.putInt(constIdx)
        pushLastChunk(Chunk(opCode, size, constIdx, c))
    }

    private fun emitGetLocal(local: Local) {
        val opCode = GET_LOCAL
        var size = byteCode.put(opCode)
        size += byteCode.putInt(local.sp)
        pushLastChunk(Chunk(opCode, size, local))
    }

    private fun emitSetLocal(local: Local) {
        val opCode = SET_LOCAL
        var size = byteCode.put(opCode)
        size += byteCode.putInt(local.sp)
        pushLastChunk(Chunk(opCode, size, local))
    }

    private fun emitJump(opCode: OpCode, location: Int? = null): Chunk {
        var size = byteCode.put(opCode)
        val offset = byteCode.size
        size += byteCode.putInt(location ?: 0)
        val chunk = Chunk(opCode, size, offset)
        pushLastChunk(chunk)
        return chunk

//        size += byteCode.emitMarkingPosition {
//            emitCode(OpCode.POP)
//            statement(true)
//        }
//        chunk.size = size
    }

    private fun patchJump(chunk: Chunk): Int {
        val offset = chunk.data[0] as Int
        val skip = byteCode.size
        byteCode.setInt(skip, offset)
        chunk.size += skip - offset
        return skip
    }

    private fun emitCall(argCount: Int) {
        val opCode = CALL
        var size = byteCode.put(opCode)
        size += byteCode.putInt(argCount)
        pushLastChunk(Chunk(opCode, size, argCount))
    }

    private fun pushLastChunk(chunk: Chunk) {
        lastChunk = chunk
        chunks.push(chunk)
    }

    private fun emitReturn() {
        emitCode(OpCode.NIL)
        emitCode(OpCode.RETURN)
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
                        var size: Int,
                        vararg val data: Any) {
        override fun toString(): String {
            return "$opCode, size: $size b, data: ${data.joinToString()}"
        }
    }

    private class ParseError(message: String) : RuntimeException(message)

    class CompilerOutput(val byteCode: ByteArray, val strings: List<String>)

    companion object {
        private const val SCRIPT = "Script"
    }
}