package net.globulus.simi.warp

import net.globulus.simi.Constants
import net.globulus.simi.Parser
import net.globulus.simi.Token
import net.globulus.simi.TokenType
import net.globulus.simi.TokenType.*
import net.globulus.simi.TokenType.CLASS
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

    private val locals = mutableListOf<Local>()
    private val upvalues = mutableListOf<Upvalue>()
    private var scopeDepth = -1 // Will be set to 0 with first beginScope()
    private val loops = Stack<ActiveLoop>()

    private var currentClass: ClassCompiler? = null

    private var lastChunk: Chunk? = null
    private val chunks: Deque<Chunk> = LinkedList()

    private var enclosing: Compiler? = null
    private lateinit var kind: String

    // Debug info
    private val debugInfoLines = mutableMapOf<Int, Int>()

    fun compile(tokens: List<Token>): Function? {
        return try {
            compileFunction(tokens, SCRIPT, 0, SCRIPT) {
                compileInternal(false)
                emitReturnNil()
            }
        } catch (e: ParseError) {
            println(e.message)
            null
        }
    }

    private fun compileFunction(tokens: List<Token>,
                                name: String,
                                arity: Int,
                                kind: String,
                                within: Compiler.() -> Unit
    ): Function {
        byteCode = mutableListOf()
        this.tokens = tokens
        current = 0
        this.kind = kind
        val selfName = if (kind != Parser.KIND_FUNCTION) Constants.SELF else ""
        locals += Local(selfName, 0, 0, false) // Stores the top-level function
        beginScope()
        this.within()
        endScope()
        printChunks(name)
        return Function(name, arity, upvalues.size, byteCode.toByteArray(), constList.toTypedArray(), DebugInfo(debugInfoLines))
    }

    /**
     * Moved to a separate fun to be able to handle syntax sugar compilations such as when.
     */
    private fun compileInternal(isExpr: Boolean) {
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
            if (isExpr) {
                expression()
            } else {
                declaration(false)
            }
        }
    }

    private fun compileNested(tokens: List<Token>, isExpr: Boolean) {
        val currentSave = current
        current = 0
        val tokensSave = this.tokens
        this.tokens = tokens + Token.copying(tokens.last(), EOF)
        compileInternal(isExpr)
        this.tokens = tokensSave
        current = currentSave
    }

    private fun beginScope() {
        scopeDepth++
    }

    /**
     * @return Number of discarded vars == number of emitted POPs
     */
    private fun endScope(emitPops: Boolean = true): Int {
        val popCount = discardLocals(scopeDepth, emitPops)
        scopeDepth--
        return popCount
    }

    private fun declaration(isExpr: Boolean): Boolean {
        updateDebugInfoLines(peek())
        return if (match(CLASS, CLASS_FINAL, CLASS_OPEN)) {
            classDeclaration()
            false
        }
        else if (match(DEF)) {
            funDeclaration()
            false
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
            statement(isExpr, lambda = false)
        }
    }

    private fun funDeclaration() {
        val function = function(Parser.KIND_FUNCTION)
        declareLocal(function.name)
    }

    private fun function(kind: String, providedName: String? = null): Function {
        val declaration = previous()
        val name = providedName ?: consumeVar("Expected an identifier for function name")
        val args = mutableListOf<String>()
        var optionalParamsStart = -1
        val defaultValues = mutableListOf<Any>()
        if (match(LEFT_PAREN)) {
            if (!check(RIGHT_PAREN)) {
                do {
                    val paramName = consumeVar("Expected param name")
                    if (match(EQUAL)) {
                        val defaultValue = consumeValue("Expected a value as default param value!")
                        if (optionalParamsStart == -1) {
                            optionalParamsStart = args.size
                        }
                        defaultValues += defaultValue
                    }
                    args += paramName
                } while (match(COMMA))
            }
            consume(RIGHT_PAREN, "Expected )")
        }

        consume(LEFT_BRACE, "Expected {")
        val curr = current
        val funcCompiler = Compiler().also {
            it.enclosing = this
            it.currentClass = currentClass
        }
        val f = funcCompiler.compileFunction(tokens, name, args.size, kind) {
            current = curr
            args.forEach { declareLocal(it) }
            block(false)
            emitReturn {
                if (kind == Parser.KIND_INIT) {
                    emitGetLocal(Constants.SELF, null) // Emit self
                } else {
                    emitCode(OpCode.NIL)
                }
            }
        }.also {
            if (optionalParamsStart != -1) {
                it.optionalParamsStart = optionalParamsStart
                it.defaultValues = defaultValues.toTypedArray()
            }
        }
        current = funcCompiler.current
        emitClosure(f, funcCompiler)
        return f
    }

    private fun classDeclaration() {
        val opener = previous()
        val name = consumeVar("Expect class name.")

        currentClass = ClassCompiler(previous(), currentClass)

        declareLocal(name)
        emitClass(name)
        if (match(LEFT_BRACE)) {
            while (!isAtEnd() && !check(RIGHT_BRACE)) {
                if (match(NEWLINE)) {
                    continue
                }
                if (match(DEF)) {
                    method()
                }
            }
            consume(RIGHT_BRACE, "\"Expect '}' after class body.\"")
        } else {
            consume(NEWLINE, "Expect newline after empty class declaration.")
        }

        currentClass = currentClass?.enclosing
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
    }

    private fun method() {
        val name = consumeVar("Expect method name.")
        val kind = if (name == Constants.INIT) Parser.KIND_INIT else Parser.KIND_METHOD
        function(kind, name)
        emitMethod(name)
    }

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

    private fun statement(isExpr: Boolean, lambda: Boolean): Boolean {
        return if (match(LEFT_BRACE)) {
            beginScope()
            block(false)
            endScope()
            false
        }
        else if (match(IF)) {
            ifSomething(false)
            false
        }
        else if (match(WHEN)) {
            whenSomething(false)
            false
        }
        else if (match(FOR)) {
            forStatement()
            false
        }
        else if (match(PRINT)) {
            printStatement()
            false
        }
        else if (match(TokenType.RETURN)) {
            returnStatement(lambda)
            false
        }
        else if (match(WHILE)) {
            whileStatement()
            false
        }
        else if (match(BREAK)) {
            breakStatement()
            false
        }
        else if (match(CONTINUE)) {
            continueStatement()
            false
        }
//        if (match(TokenType.RESCUE)) {
//            return rescueStatement()
//        }
//        if (match(TokenType.YIELD)) {
//            return yieldStatement(lambda)
//        }
        else {
            return expressionStatement(lambda)
        }
    }

    private fun block(isExpr: Boolean) {
        var lastLineIsExpr = false
        while (!check(RIGHT_BRACE)) {
            matchAllNewlines()
            lastLineIsExpr = declaration(isExpr)
            matchAllNewlines()
        }
        consume(RIGHT_BRACE, "Expect '}' after block!")
        if (isExpr && !lastLineIsExpr) {
            throw error(previous(), "Block is not a valid expression block!")
        }
    }

    private fun ifSomething(isExpr: Boolean) {
        val opener = previous()
        expression()
        val ifChunk = emitJump(JUMP_IF_FALSE)
        emitCode(POP)
        compileIfBody(opener, isExpr)
        val elseJump = emitJump(JUMP)
        patchJump(ifChunk)
        emitCode(POP)
        matchAllNewlines()
        if (match(ELSE)) {
            compileIfBody(opener, isExpr)
        } else if (isExpr) {
            throw error(opener, "An if expression must have an else!")
        }
        patchJump(elseJump)
    }

    private fun compileIfBody(opener: Token, isExpr: Boolean) {
        if (isExpr) {
            val isRealExpr = expressionOrExpressionBlock() // Assignments can be statements, we can't know till we parse
            if (!isRealExpr) {
                throw error(opener, "An if expression must have a ")
            }
        } else {
            statement(isExpr = false, lambda = true)
        }
    }

    private fun expressionOrExpressionBlock(): Boolean {
        return if (match(LEFT_BRACE)) {
            beginScope()
            block(true)
            val popCount = endScope(false)
            if (lastChunk?.opCode != POP) {
                throw IllegalStateException("Compiler error - last chunk should be POP at this point!")
            }
            rollBackLastChunk()
            emitPopUnder(popCount)
            true
        } else {
            expression()
        }
    }

    /**
     * Actually creates a list of tokens that represent if-else-ifs that are then compiled internally
     */
    private fun whenSomething(isExpr: Boolean) {
        val origin = previous()
        val id = consume(IDENTIFIER, "Expected an identifier after 'when'")
        var first = true
        var wroteElse = false
        val whenTokens = mutableListOf<Token>()
        consume(LEFT_BRACE, "Expect a '{' after when")
        consume(NEWLINE, "Expect a newline after when '{'")
        val consumeConditionBlock: () -> List<Token> = if (isExpr) {
            { consumeUntilType(OR, LEFT_BRACE, EQUAL) }
        } else {
            { consumeUntilType(OR, LEFT_BRACE) }
        }
        val conditionAtEndBlock: () -> Boolean = if (isExpr) {
            { check(LEFT_BRACE, EQUAL) }
        } else {
            { check(LEFT_BRACE) }
        }

        while (!isAtEnd() && !check(RIGHT_BRACE)) {
            matchAllNewlines()
            if (match(ELSE)) {
                wroteElse = true
                whenTokens += previous()
                whenTokens += consumeNextBlock(isExpr)
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
                    whenTokens += consumeConditionBlock()

                    // If we've found an or, we just take it as-is
                    if (match(OR)) {
                        whenTokens += previous()
                    }
                } while (!conditionAtEndBlock())

                // Consume the statement
                whenTokens += consumeNextBlock(isExpr)
            }
            matchAllNewlines()
        }
        consume(RIGHT_BRACE, "Expect '}' at the end of when")
        compileNested(whenTokens, isExpr)
    }

    private fun forStatement() {
        val opener = previous()
        val id = consume(IDENTIFIER, "Expect an identifier after 'for'.")
        consume(IN, "Expect 'in' after identifier in 'for'.")
        val iterableTokens = consumeUntilType(LEFT_BRACE)
        val blockTokens = consumeNextBlock(false)
        val tokens = mutableListOf<Token>().apply {
            val iterator = Token.named("__iterator__${System.currentTimeMillis()}")
            val eq = Token.ofType(EQUAL)
            val lp = Token.ofType(LEFT_PAREN)
            val rp = Token.ofType(RIGHT_PAREN)
            val dot = Token.ofType(DOT)
            val nl = Token.ofType(NEWLINE)
            add(iterator)
            add(eq)
            add(lp)
            addAll(iterableTokens)
            add(rp)
            add(dot)
            add(Token.named(Constants.ITERATE))
            add(lp)
            add(rp)
            add(nl)

            add(Token.ofType(WHILE))
            add(iterator)
            for ((i, blockToken) in blockTokens.withIndex()) {
                add(blockToken)
                if (i == 0) {
                    add(nl)
                    add(id)
                    add(eq)
                    add(iterator)
                    add(dot)
                    add(Token.named(Constants.NEXT))
                    add(lp)
                    add(rp)
                    add(nl)

                    add(Token.ofType(IF))
                    add(id)
                    add(Token.ofType(EQUAL_EQUAL))
                    add(Token.ofType(NIL))
                    add(Token.ofType(BREAK))
                    add(nl)
                }
            }
        }
        compileNested(tokens, false)
    }

    private fun printStatement() {
        expression()
        emitCode(OpCode.PRINT)
    }

    private fun returnStatement(lambda: Boolean) {
        if (kind == Parser.KIND_INIT) {
            throw error(previous(), "Can't return from init!")
        }
        if (!match(NEWLINE)) {
            expression()
            checkIfUnidentified()
        }
        consume(NEWLINE, "Expected newline after return value.")
        emitCode(OpCode.RETURN)
        // After return is emitted, we need to close the scope, so we emit the number of additional
        // instructions to interpret before we actually return from call frame
        val pos = byteCode.size
        byteCode.putInt(0)
        val popCount = endScope()
        byteCode.setInt(popCount, pos)
    }

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
        statement(isExpr = false, lambda = true)
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
            discardLocals(depth, emitPops = true, keepLocals = true)
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
            discardLocals(depth, emitPops = true, keepLocals = true)
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

    private fun expressionStatement(lambda: Boolean): Boolean {
        val shouldPop = expression()
        if (shouldPop) {
            emitCode(POP)
        }
        return shouldPop
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
                if (lastChunk?.opCode == GET_PROP) {
                    rollBackLastChunk()
                    or()
                    emitCode(SET_PROP)
                } else {
                    if (lastChunk?.opCode == GET_UPVALUE) {
                        val name = lastChunk!!.data[2] as String
                        warn(previous(), "Name shadowed: $name.")
                        declareLocal(name)
                        rollBackLastChunk()
                    } else if (lastChunk?.opCode != CONST_ID) {
                        throw error(equals, "Expected an ID for var declaration!")
                    } else {
                        declareLocal(lastChunk!!.data[1] as String)
                        rollBackLastChunk()
                    }
                    or() // push the value on the stack
                }
            } else {
                val variable: Any = when (lastChunk?.opCode) {
                    GET_LOCAL -> lastChunk!!.data[1] as Local
                    GET_UPVALUE -> lastChunk!!.data[0] as Int
                    else -> throw error(equals, "Assigning to undeclared var!")
                }
                if (equals.type == QUESTION_QUESTION_EQUAL) {
                    nilCoalescenceWithKnownLeft { or() }
                } else {
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
                        })
                    }
                }
                emitSet(variable)
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
            when (operator.type) {
                EQUAL_EQUAL -> emitCode(EQ)
                BANG_EQUAL -> {
                    emitCode(EQ)
                    emitCode(INVERT)
                }
                else -> throw IllegalArgumentException("WTF")
            }
        }
    }

    private fun comparison() {
        range()
        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL/*, LESS_GREATER*/)) {
            val operator = previous()
            range()
            emitCode(when (operator.type) {
                GREATER -> GT
                GREATER_EQUAL -> GE
                LESS -> LT
                LESS_EQUAL -> LE
                else -> throw IllegalArgumentException("WTF")
            })
        }
    }

    private fun range() {
        addition()
        while (match(DOT_DOT, DOT_DOT_DOT)) {
            val operator = previous()
            addition()
//            emitCode(when (operator.type) {
//                DOT_DOT -> dsfds
//            })
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
        unary()
        while (match(QUESTION_QUESTION)) {
            nilCoalescenceWithKnownLeft { unary() }
        }
    }

    private fun nilCoalescenceWithKnownLeft(right: () -> Unit) {
        val elseChunk = emitJump(JUMP_IF_NIL)
        val endChunk = emitJump(JUMP)
        patchJump(elseChunk)
        emitCode(POP)
        right()
        patchJump(endChunk)
    }

    private fun unary() {
        if (match(NOT, MINUS, BANG_BANG)) {
            val operator = previous()
            unary()
            emitCode(when (operator.type) {
                NOT -> INVERT
                MINUS -> NEGATE
                else -> throw IllegalArgumentException("WTF")
            })
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
        primary()
        while (true) {
            if (match(LEFT_PAREN, DOLLAR_LEFT_PAREN)) {
                finishCall()
            } else if (match(DOT)) {
                if (match(CLASS)) {
                    emitId("class")
                } else {
                    primary()
                }
                if (match(LEFT_PAREN)) {
                    val name = lastChunk!!.data[if (lastChunk?.opCode == GET_LOCAL) 0 else 1] as String
                    rollBackLastChunk()
                    val argCount = argList()
                    emitInvoke(name, argCount)
                } else {
                    if (lastChunk?.opCode == GET_LOCAL) {
                        val name = lastChunk!!.data[0] as String
                        rollBackLastChunk()
                        emitId(name)
                    }
                    emitCode(GET_PROP)
                }
            } else {
                break
            }
        }
    }

    private fun finishCall() {
        checkIfUnidentified()
        val argCount = argList()
        emitCall(argCount)
    }

    private fun argList(): Int {
        var count = 0
        if (!check(RIGHT_PAREN)) {
            do {
                count++
                matchSequence(IDENTIFIER, EQUAL) // allows for named params, e.g substr(start=1,end=2)
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
        else if (match(SELF)) {
            if (currentClass == null) {
                throw error(previous(), "Cannot use 'self' outside of class.")
            }
            variable()
//            val previous = previous()
//            var specifier: Token? = null
//            if (peekSequence(TokenType.LEFT_PAREN, TokenType.DEF, TokenType.RIGHT_PAREN)) {
//                specifier = Token(TokenType.DEF, Constants.SELF_DEF, null, previous!!.line, previous.file)
//                advance()
//                advance()
//                advance()
//            }
//            return Self(Token(TokenType.SELF, Constants.SELF, null, previous!!.line, previous.file), specifier)
        }
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
            variable()
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
        else if (match(IF)) {
            return ifSomething(true)
        }
        else if (match(WHEN)) {
            return whenSomething(true)
        }
        else {
            throw error(peek(), "Expect expression.")
        }
    }

    private fun variable() {
        val id = previous()
        val name = id.lexeme
        val local = findLocal(this, name)
        if (local != null) {
            emitGetLocal(name, local)
        } else {
            val upvalue = resolveUpvalue(this, name)
            if (upvalue != null) {
                emitGetUpvalue(name, upvalue)
            } else {
                emitId(name)
            }
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

    private fun consumeNextBlock(isExpr: Boolean): List<Token> {
        if (isExpr && match(EQUAL)) {
            val start = current
            while (!match(NEWLINE)) {
                current++
            }
            return tokens.subList(start, current)
        } else {
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
    }

    private fun consumeUntilType(vararg types: TokenType): List<Token> {
        val start = current
        while (!isAtEnd() && !check(*types)) {
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

    private fun consumeVar(message: String): String {
        return consume(IDENTIFIER, message).lexeme
    }

    private fun consumeValue(message: String): Any {
        return when {
            match(FALSE) -> 0L
            match(TRUE) -> 1L
            match(NIL) -> Nil
            match(NUMBER) -> {
                val num = previous().literal.number
                if (num.isInteger) {
                    num.asLong()
                } else {
                    num.asDouble()
                }
            }
            match(STRING) -> previous().literal.string
            else -> throw error(peek(), message)
        }
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
        return current == tokens.size || peek().type == EOF
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
        return ParseError(bundleTokenWithMessage(token, message) + TokenPatcher.patch(tokens, token))
    }

    private fun warn(token: Token, message: String) {
        println("w: ${bundleTokenWithMessage(token, message)}")
    }

    private fun bundleTokenWithMessage(token: Token, message: String): String {
        return "[\"${token.file}\" line ${token.line}] At ${token.type}: $message\n"
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

    private fun emitPopUnder(count: Int) {
        var size = byteCode.put(POP_UNDER)
        size += byteCode.putInt(count)
        pushLastChunk(Chunk(POP_UNDER, size, count))
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

    private fun emitConstWithIndex(c: Const) {
        var size = byteCode.put(CONST)//if (c.level == -1) CONST else CONST_OUTER)
        if (c.level != -1) {
            size += byteCode.putInt(c.level)
        }
        size += byteCode.putInt(c.index)
        pushLastChunk(Chunk(CONST, size, c.index, c.level))
    }

    private fun emitIdOrConst(opCode: OpCode, c: Any) {
        var size = byteCode.put(opCode)
        val constIdx = constIndex(c)
        size += byteCode.putInt(constIdx)
        pushLastChunk(Chunk(opCode, size, constIdx, c))
    }

    private fun emitClosure(f: Function, funcCompiler: Compiler) {
        val opCode = CLOSURE
        var size = byteCode.put(opCode)
        val constIdx = constIndex(f)
        size += byteCode.putInt(constIdx)
        for (i in 0 until f.upvalueCount) {
            val upvalue = funcCompiler.upvalues[i]
            size += byteCode.put((if (upvalue.isLocal) 1 else 0).toByte())
            size += byteCode.putInt(upvalue.sp)
        }
        pushLastChunk(Chunk(opCode, size, constIdx, f))
    }

    private fun emitClass(name: String) {
        val opCode = OpCode.CLASS
        var size = byteCode.put(opCode)
        val constIdx = constIndex(name)
        size += byteCode.putInt(constIdx)
        pushLastChunk(Chunk(opCode, size, constIdx, name))
    }

    private fun emitMethod(name: String) {
        val opCode = OpCode.METHOD
        var size = byteCode.put(opCode)
        val constIdx = constIndex(name)
        size += byteCode.putInt(constIdx)
        pushLastChunk(Chunk(opCode, size, constIdx, name))
    }

    private fun emitGetLocal(name: String, local: Local?) {
        val opCode = GET_LOCAL
        var size = byteCode.put(opCode)
        size += byteCode.putInt(local?.sp ?: 0)
        pushLastChunk(Chunk(opCode, size, name, local ?: name))
    }

    private fun emitGetUpvalue(name: String, upvalue: Upvalue) {
        val opCode = GET_UPVALUE
        val index = upvalues.indexOf(upvalue)
        var size = byteCode.put(opCode)
        size += byteCode.putInt(index)
        pushLastChunk(Chunk(opCode, size, index, upvalue, name))
    }

    private fun emitSet(it: Any) {
        when (it) {
            is Local -> emitSetLocal(it)
            is Int -> emitSetUpvalue(it)
            else -> throw IllegalArgumentException("WTF")
        }
    }

    private fun emitSetLocal(local: Local) {
        val opCode = SET_LOCAL
        var size = byteCode.put(opCode)
        size += byteCode.putInt(local.sp)
        pushLastChunk(Chunk(opCode, size, local))
    }
    private fun emitSetUpvalue(index: Int) {
        val opCode = SET_UPVALUE
        var size = byteCode.put(opCode)
        size += byteCode.putInt(index)
        pushLastChunk(Chunk(opCode, size, index, upvalues[index]))
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

    private fun emitInvoke(name: String, argCount: Int) {
        val opCode = INVOKE
        var size = byteCode.put(opCode)
        val constIdx = constIndex(name)
        size += byteCode.putInt(constIdx)
        size += byteCode.putInt(argCount)
        pushLastChunk(Chunk(opCode, size, name, argCount, constIdx))
    }

    private fun pushLastChunk(chunk: Chunk) {
        lastChunk = chunk
        chunks.push(chunk)
    }

    private fun emitReturn(value: () -> Unit) {
        value()
        emitCode(OpCode.RETURN)
        byteCode.putInt(0) // No additional instructions to skip
    }

    private fun emitReturnNil() {
        emitReturn {
            emitCode(OpCode.NIL)
        }
    }

    private fun updateDebugInfoLines(token: Token) {
        debugInfoLines.putIfAbsent(token.line, byteCode.size)
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
        val l = Local(name, end, scopeDepth, false)
        locals += l
        return l
    }

    private fun findLocal(compiler: Compiler, name: String): Local? {
//        var compiler: Compiler? = this
//        while (compiler != null) {
            for (i in compiler.locals.size - 1 downTo 0) {
                val local = compiler.locals[i]
                if (local.name == name) {
//                    return if (compiler == this) {
                        return local
//                    } else {
//                        local.copyForLevel(compiler.numberOfEnclosingCompilers)
//                    }
                }
            }
//            compiler = compiler.enclosing
//        }
        return null
    }

    /**
     * @return The number of popping statements emitted
     */
    private fun discardLocals(depth: Int, emitPops: Boolean = true, keepLocals: Boolean = false): Int {
        var popCount = 0
        var i = locals.size - 1
        while (i >= 0 && locals[i].depth == depth) {
            if (emitPops) {
                popCount++
                if (locals[i].isCaptured) {
                    emitCode(CLOSE_UPVALUE)
                } else {
                    emitCode(POP)
                }
            }
            if (!keepLocals) {
                locals.removeAt(i)
            }
            i--
        }
        return popCount
    }

    private fun resolveUpvalue(compiler: Compiler, name: String): Upvalue? {
        if (compiler.enclosing == null) {
            return null
        }
        val local = findLocal(compiler.enclosing!!, name)
        if (local != null) {
            local.isCaptured = true
            return addUpvalue(compiler, local.sp, true)
        }
        val upvalue = resolveUpvalue(compiler.enclosing!!, name)
        if (upvalue != null) {
            return addUpvalue(compiler, upvalue.sp, false)
        }
        return null
    }

    private fun addUpvalue(compiler: Compiler, sp: Int, isLocal: Boolean): Upvalue {
        for (upvalue in compiler.upvalues) {
            if (upvalue.sp == sp && upvalue.isLocal == isLocal) {
                return upvalue
            }
        }
        val upvalue = Upvalue(sp, isLocal)
        compiler.upvalues += upvalue
        return upvalue
    }

    private fun printChunks(name: String) {
        val tabLevel = numberOfEnclosingCompilers
        if (tabLevel > 0) {
            println("  ".repeat(tabLevel - 1) + "in $name")
        }
        val tabs = "  ".repeat(tabLevel)
        while (chunks.isNotEmpty()) {
            println(tabs + chunks.last)
            chunks.removeLast()
        }
    }

    private val numberOfEnclosingCompilers: Int get() {
        var count = 0
        var compiler = this.enclosing
        while (compiler != null) {
            count++
            compiler = compiler.enclosing
        }
        return count
    }

    private fun checkIfUnidentified() {
        if (lastChunk?.opCode == CONST_ID) {
            throw error(previous(), "Unable to resolve identifier: ${lastChunk!!.data[1]}")
        }
    }

    private data class Const(val index: Int,
                             val level: Int
    )

    private data class Local(val name: String,
                             val sp: Int,
                             val depth: Int,
                             var isCaptured: Boolean
    )

    private data class Upvalue(val sp: Int,
                               val isLocal: Boolean
    )

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

    private class ClassCompiler(val name: Token,
                                val enclosing: ClassCompiler?
    )

    private class ParseError(message: String) : RuntimeException(message)

    companion object {
        private const val SCRIPT = "Script"
    }
}