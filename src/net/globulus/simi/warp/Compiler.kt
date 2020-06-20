package net.globulus.simi.warp

import net.globulus.simi.Constants
import net.globulus.simi.Parser
import net.globulus.simi.Token
import net.globulus.simi.TokenType
import net.globulus.simi.TokenType.*
import net.globulus.simi.TokenType.CLASS
import net.globulus.simi.TokenType.IS
import net.globulus.simi.TokenType.MOD
import net.globulus.simi.TokenType.NIL
import net.globulus.simi.TokenType.PRINT
import net.globulus.simi.api.SimiValue
import net.globulus.simi.tool.TokenPatcher
import net.globulus.simi.warp.OpCode.*
import net.globulus.simi.warp.debug.DebugInfo
import net.globulus.simi.warp.native.NativeFunction
import net.globulus.simi.warp.native.NativeModuleLoader
import java.util.*
import java.util.regex.Pattern

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

    private var lambdaCounter = 0
    private var implicitVarCounter = 0
    private var returnVerificationVarCounter = 0
    private var annotationCounter = 0 // counts the number of emitted annotations to see if they're placed properly
    private var callArgumentsCounter = 0 // counts the number of started call arguments lists, to track if * can be used to spread an object
    private val totalCallArgumentCounter: Int get() = callArgumentsCounter + (enclosing?.totalCallArgumentCounter ?: 0)

    private var currentClass: ClassCompiler? = null
    private val compiledClasses = mutableMapOf<String, ClassCompiler>()

    private var lastChunk: Chunk? = null
    private val chunks = mutableListOf<Chunk>()

    private var enclosing: Compiler? = null
    private lateinit var kind: String
    private lateinit var fiberName: String
    // If the function being compiled was marked with "is Type", we need to verify its return statements
    private var verifyReturnType: DynamicTypeCheck? = null

    // Debug info
    private val debugInfoLines = mutableMapOf<Int, Int>()
    private val debugInfoBreakpoints = mutableListOf<Int>()

    private val numberOfEnclosingCompilers: Int by lazy {
        var count = 0
        var compiler = this.enclosing
        while (compiler != null) {
            count++
            compiler = compiler.enclosing
        }
        count
    }

    fun compile(tokens: List<Token>): Function {
        val name = SCRIPT + System.currentTimeMillis()
        return compileFunction(tokens, name, 0, name) {
            fiberName = name
            compileInternal(false)
            emitReturnNil()
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
        locals += Local(selfName, 0, 0, isCaptured = false) // Stores the top-level function
        beginScope()
        this.within()
        val debugInfoLocals = locals.map { it.sp to it.name }.toMap()
        endScope()
//        printChunks(name)
        return Function(name, arity, upvalues.size, byteCode.toByteArray(), constList.toTypedArray(),
                DebugInfo(debugInfoLines, debugInfoLocals, debugInfoBreakpoints, tokens)
        )
    }

    /**
     * Moved to a separate fun to be able to handle syntax sugar compilations such as when.
     */
    private fun compileInternal(isExpr: Boolean) {
        while (!isAtEnd()) {
            if (match(NEWLINE)) {
                continue
            }
            if (isExpr) {
                expression()
            } else {
                declaration()
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
    private fun endScope(emitPops: Boolean = true, keepLocals: Boolean = false): Int {
        val popCount = discardLocals(scopeDepth, emitPops, keepLocals)
        scopeDepth--
        assert(scopeDepth > -1)
        return popCount
    }

    private fun declaration(): Boolean {
        updateDebugInfo(peek)
        return if (match(HASH)) {
            internalDirective()
            false
        } else if (matchClassOpener()) {
            classDeclaration(false)
            false
        } else if (match(IMPORT)) {
            checkAnnotationCounter()
            import()
            false
        } else if (match(FN)) {
            checkAnnotationCounter()
            funDeclaration()
            false
        } else if (match(FIB)) {
            checkAnnotationCounter()
            fiber()
            false
        } else if (match(BANG)) {
            annotation()
            false
        } else if (match(TokenType.EXTEND)) {
            checkAnnotationCounter()
            extension()
            false
        } else {
            checkAnnotationCounter()
            statement()
        }
    }

    private fun funDeclaration(): String {
        val function = function(Parser.KIND_FUNCTION, true)
        return function.name
    }

    private fun fiber(): String {
        val fiber = function(Parser.KIND_FIBER, true)
        return fiber.name
    }

    private fun function(kind: String, declareLocal: Boolean, providedName: String? = null): Function {
        val declaration = if (current > 0) previous else tokens[0] // current can be 0 in a gu expression
        val name = providedName ?: consumeVar("Expected an identifier for function name")
        if (declareLocal) {
            declareLocal(name, true)
        }
        val (args, prependedTokens, optionalParamsStart, defaultValues) = scanArgs(kind, true)

        // the init method also initializes all the fields declard in the class
        if (kind == Parser.KIND_INIT) {
            prependedTokens += currentClass!!.initAdditionalTokens
        }

        var returnType: DynamicTypeCheck? = null
        if (match(IS)) {
            when (kind) {
                Parser.KIND_LAMBDA -> throw error(previous, "Can't specify return type of lambdas!")
                Parser.KIND_INIT -> throw error(previous, "Can't specify return type of init!")
                else -> returnType = consumeDynamicTypeCheck("Expect type.")
            }
        }

        var isExprFunc = false
        var hasBody = true
        if (match(EQUAL)) {
            isExprFunc = true
        } else if (!match(LEFT_BRACE)) {
            hasBody = false
            consume(NEWLINE, "Expect '{', '=' or newline to start func.")
        }

        if (kind == Parser.KIND_LAMBDA && hasBody && args.isEmpty()) {
            // Scan for implicit args
            args += scanForImplicitArgs(declaration, isExprFunc)
        }

        val curr = current
        val funcCompiler = Compiler().also {
            it.enclosing = this
            it.currentClass = currentClass
            it.verifyReturnType = returnType
        }
        val f = funcCompiler.compileFunction(tokens, name, args.size, kind) {
            fiberName = if (kind == Parser.KIND_FIBER) name else this@Compiler.fiberName
            current = curr
            args.forEach { declareLocal(it) }
            if (isExprFunc) {
                expression()
                emitReturn { }
            } else {
                if (prependedTokens.isNotEmpty()) {
                    compileNested(prependedTokens, false)
                }
                if (hasBody) {
                    block(false)
                }
                emitReturn {
                    if (kind == Parser.KIND_INIT) {
                        emitGetLocal(Constants.SELF, null) // Emit self
                    } else {
                        emitCode(OpCode.NIL)
                    }
                }
            }
        }.also {
            if (optionalParamsStart != -1) {
                it.optionalParamsStart = optionalParamsStart
                it.defaultValues = defaultValues.toTypedArray()
            }
        }
        current = funcCompiler.current
        emitClosure(f, funcCompiler, kind == Parser.KIND_FIBER)

        return f
    }

    private fun scanArgs(kind: String, allowPrepend: Boolean): ArgScanResult {
        val args = mutableListOf<String>()
        val prependedTokens = mutableListOf<Token>()
        var optionalParamsStart = -1
        val defaultValues = mutableListOf<Any>()
        if (match(LEFT_PAREN)) {
            if (!check(RIGHT_PAREN)) {
                do {
                    var initAutoset = false
                    if (matchSequence(SELF, DOT)) {
                        if (!allowPrepend) {
                            throw error(previous, "Autoset arguments aren't allowed here.")
                        } else if (kind == Parser.KIND_INIT) {
                            initAutoset = true
                        } else {
                            throw error(previous, "Autoset arguments are only allowed in initializers!")
                        }
                    }
                    val paramName = consumeVar("Expect param name.")
                    val nameToken = Token.named(paramName)
                    if (match(IS)) {
                        if (!allowPrepend) {
                            throw error(previous, "Argument type specifiers aren't allowed here.")
                        }
                        val type = consumeDynamicTypeCheck("Expect type.")
                        prependedTokens += getTypeVerificationTokens(nameToken, type)
                    }
                    if (match(EQUAL)) {
                        val defaultValue = consumeValue("Expected a value as default param value!")
                        if (optionalParamsStart == -1) {
                            optionalParamsStart = args.size
                        }
                        defaultValues += defaultValue
                    }
                    args += paramName
                    if (initAutoset) {
                        prependedTokens += listOf(Token.self(), Token.ofType(DOT), nameToken,
                                Token.ofType(EQUAL), nameToken, Token.ofType(NEWLINE))
                    }
                } while (match(COMMA))
            }
            consume(RIGHT_PAREN, "Expected )")
        }
        return ArgScanResult(args, prependedTokens, optionalParamsStart, defaultValues)
    }

    /**
     * Parses internal directives left for the compiler by the compiler, usually by synthesizing HASH tokens in a token
     * list meant to be compiled with [compileNested].
     */
    private fun internalDirective() {
        when (consumeVar("Expect action after #.")) {
            ADD_TO_COMPREHENSION -> {
                // The last line was (or should've been) a statement line, meaning it ends in a POP. Roll that pop back
                // and replace it with an ADD_TO_COMPREHENSION, which also pops.
                if (lastChunk?.opCode == POP) {
                    rollBackLastChunk()
                    emitCode(OpCode.ADD_TO_COMPREHENSION)
                } else {
                    throw error(previous, "Expect adding to comprehension to be preceded by a POP.")
                }
            }
            else -> throw IllegalArgumentException("WTF")
        }
    }

    /**
     * @return The name of the class
     */
    private fun classDeclaration(inner: Boolean): String {
        resetAnnotationCounter()
        val opener = previous
        val kind = when (opener.type) {
            MODULE -> SClass.Kind.MODULE
            ENUM -> SClass.Kind.ENUM
            CLASS_FINAL -> SClass.Kind.FINAL
            CLASS -> SClass.Kind.REGULAR
            CLASS_OPEN -> SClass.Kind.OPEN
            else -> throw IllegalStateException("WTF")
        }
        var name = consumeVar("Expect class name.")
        val returnName = name // it's different because inner classes have the name prepended
        if (inner) {
            name = "${currentClass!!.name}.$name"
        }
        declareLocal(name, true)
        emitClass(name, kind, inner)
        val superclasses = mutableListOf<String>()
        currentClass = ClassCompiler(previous.lexeme, currentClass)

        if (match(IS)) { // Superclasses
            do {
                val superclassName = consumeVar("Expect superclass name.")
                if (superclassName == name) {
                    throw error(previous, "A class cannot inherit from itself!")
                }
                if (superclassName != Constants.CLASS_OBJECT) { // All classes inherit from Object
                    superclasses += superclassName
                }
            } while (match(COMMA))
        }
        if (kind != SClass.Kind.MODULE
                && superclasses.isEmpty()
                && name != Constants.CLASS_OBJECT
                && name != Constants.CLASS_FUNCTION
                && name != Constants.CLASS_CLASS) {
            superclasses += Constants.CLASS_OBJECT
        }
        currentClass?.superclasses = superclasses
        currentClass?.firstSuperclass = superclasses.firstOrNull()

        // Reverse the superclasses so that the first one listed is the last one inherited, meaning
        // its fields override those of lesser-ranked superclasses
        superclasses.reversed().forEach {
            variable(it)
            emitCode(INHERIT)
        }

        if (match(IMPORT)) { // Mixins
            val mixins = mutableListOf<String>()
            do {
                val mixinName = consumeVar("Expect mixin name.")
                if (mixinName == name) {
                    throw error(previous, "A class cannot mix itself in!")
                }
                mixins += mixinName
            } while (match(COMMA))
            // Reverse the mixins for the same reason we did with superclasses
            mixins.reversed().forEach {
                variable(it)
                emitCode(MIXIN)
            }
        }

        if (match(LEFT_BRACE)) {
            if (kind == SClass.Kind.ENUM) {
                compileEnumValues()
                consume(NEWLINE, "Expect newline after enum values.")
            }
            var hasInit = false
            while (!isAtEnd() && !check(RIGHT_BRACE)) {
                if (match(NEWLINE)) {
                    continue
                }
                if (matchClassOpener()) {
                    if (previous.type == MODULE && kind != SClass.Kind.MODULE) {
                        throw error(previous, "Can't place a module inside a class!")
                    }
                    val className = classDeclaration(true)
                    currentClass?.declaredFields?.add(className)
                } else if (match(FN, FIB)) {
                    val methodName = methodOrFiber(previous.type == FIB, false)
                    currentClass?.declaredFields?.add(methodName)
                } else if (match(NATIVE)) {
                    val methodName = nativeMethod(false)
                    currentClass?.declaredFields?.add(methodName)
                } else if (match(BANG)) {
                    annotation()
                } else if (match(IDENTIFIER)) {
                    val fieldToken = previous
                    val fieldName = fieldToken.lexeme
                    currentClass?.declaredFields?.add(fieldName)
                    if (fieldName == Constants.INIT) {
                        methodOrFiber(isFiber = false, isExtension = false, providedName = fieldName)
                        hasInit = true
                    } else if (match(EQUAL)) {
                        if (kind == SClass.Kind.MODULE) {
                            firstNonAssignment(true)
                            emitField(fieldName)
                            consume(NEWLINE, "Expect newline after class field declaration.")
                        } else {
                            currentClass?.initAdditionalTokens?.apply {
                                addAll(listOf(Token.self(), Token.ofType(DOT), fieldToken, previous))
                                addAll(consumeNextExpr())
                                add(consume(NEWLINE, "Expect newline after class field declaration."))
                            }
                        }
                        if (annotationCounter > 0) {
                            emitAnnotateField(fieldName)
                            resetAnnotationCounter()
                        }
                    } else {
                        throw error(previous, "Invalid line in class declaration.")
                    }
                }
            }
            consume(RIGHT_BRACE, "\"Expect '}' after class body.\"")

            // If the class declares fields but not an init, we need to synthesize one
            if (currentClass?.initAdditionalTokens?.isNotEmpty() == true && !hasInit) {
                methodOrFiber(isFiber = false, isExtension = false, providedName = Constants.INIT)
            }
        } else {
            when (kind) {
                SClass.Kind.ENUM -> throw error(peek, "Enum must have a body.")
                SClass.Kind.MODULE -> throw error(peek, "This is kind of a useless module, don't you think?")
                else -> consume(NEWLINE, "Expect newline after empty class declaration.")
            }
        }

        emitCode(CLASS_DECLR_DONE)
        if (kind == SClass.Kind.ENUM) {
            emitCode(INIT_ENUM)
        }
        compiledClasses[name] = currentClass!!
        currentClass = currentClass?.enclosing
        return returnName
    }

    private fun compileEnumValues() {
        val tokens = mutableListOf<Token>()
        val selfDot = listOf(Token.self(), Token.ofType(DOT))
        val lprp = listOf(Token.ofType(LEFT_PAREN), Token.ofType(RIGHT_PAREN))
        val equalsSelf = listOf(Token.ofType(EQUAL), Token.self())
        val nl = Token.ofType(NEWLINE)
        do {
            matchAllNewlines()
            val id = consume(IDENTIFIER, "Expect identifier for enum value.")
            tokens += selfDot
            tokens += id
            tokens += equalsSelf
            tokens += if (peek.type == LEFT_PAREN) { // Found an constructor invocation
                consumeArgList()
            } else {
                lprp
            }
            tokens += nl
        } while (match(COMMA))
        val compiler = Compiler().also {
            it.enclosing = this
            it.currentClass = currentClass
        }
        val f = compiler.compileFunction(tokens, Constants.ENUM_INIT, 0, Parser.KIND_METHOD) {
            compileInternal(false)
            emitReturnNil()
        }
        emitClosure(f, compiler, false)
        emitMethod(Constants.ENUM_INIT, false)
    }

    private fun methodOrFiber(isFiber: Boolean, isExtension: Boolean, providedName: String? = null): String {
        resetAnnotationCounter()
        val name = providedName ?: consumeVar("Expect method name.")
        val kind = if (name == Constants.INIT)
                Parser.KIND_INIT
            else {
                if (isFiber)
                    Parser.KIND_FIBER
                else
                    Parser.KIND_METHOD
            }
        function(kind, false, name)
        emitMethod(name, isExtension)
        return name
    }

    private fun nativeMethod(isExtension: Boolean): String {
        resetAnnotationCounter()
        val name = consumeVar("Expect method name.")
        val (args, _, optionalParamsStart, defaultValues) = scanArgs(Parser.KIND_METHOD, false)
        consume(NEWLINE, "Expect newline after native method declaration.")
        val nativeFunction = NativeModuleLoader.resolve(currentClass!!.name, name)?.also {
            it.optionalParamsStart = optionalParamsStart
            it.defaultValues = defaultValues.toTypedArray()
        } ?: throw error(previous, "Can't resolve native function $name.")
        if (nativeFunction.arity != args.size) {
            throw error(previous, "Native function $name expects ${nativeFunction.arity} args but got ${args.size}.")
        }
        emitNativeMethod(name, nativeFunction, isExtension)
        return name
    }

    private fun import() {
        // match(STRING) imports were resolved at scanning phase, this is only a import _ for _, _
        firstNonAssignment(true)
        val moduleLocal = declareLocal(nextImplicitVarName("module"))
        consume(FOR, "Expect 'for' after module name.")
        do {
            val field = consumeVar("Expect identifier to specify imported field.")
            declareLocal(field, true)
            emitGetLocal(moduleLocal.name, moduleLocal)
            emitId(field)
            finishGet(false)
        } while (match(COMMA))
        consume(NEWLINE, "Expect newline after on-site import.")
    }

    private fun annotation() {
        if (match(LEFT_BRACKET)) {
            objectLiteral(previous)
        } else if (peek.type == IDENTIFIER) {
            call(true)
        } else {
            throw error(peek, "Annotation must include either an object literal or a constructor invocation!")
        }
        consume(NEWLINE, "Expect newline after annotation.")
        emitCode(ANNOTATE)
        annotationCounter++
    }

    private fun extension() {
        val name = consumeVar("Expected the name of the class being extended.")
        variable(name)
        checkIfUnidentified()
        currentClass = compiledClasses[name]?.apply {
            enclosing = currentClass
        } ?: throw error(previous, "Class not previously compiled: $name.")
        emitCode(OpCode.EXTEND)
        if (match(IMPORT)) { // Mixins
            val mixins = mutableListOf<String>()
            do {
                val mixinName = consumeVar("Expect mixin name.")
                if (mixinName == name) {
                    throw error(previous, "A class cannot mix itself in!")
                }
                mixins += mixinName
            } while (match(COMMA))
            // Reverse the mixins for the same reason we did with superclasses
            mixins.reversed().forEach {
                variable(it)
                emitCode(EXTEND_MIXIN)
            }
        }
        if (match(LEFT_BRACE)) {
            while (!isAtEnd() && !check(RIGHT_BRACE)) {
                if (match(NEWLINE)) {
                    continue
                }
                if (match(FN)) {
                    val methodName = methodOrFiber(isFiber = false, isExtension = true)
                    currentClass?.declaredFields?.add(methodName)
                } else if (match(NATIVE)) {
                    nativeMethod(true)
                } else if (match(BANG)) {
                    annotation()
                } else {
                    throw error(previous, "Invalid line in extension declaration.")
                }
            }
            consume(RIGHT_BRACE, "\"Expect '}' after extension body.\"")
        }
        currentClass = currentClass?.enclosing
        emitCode(EXTEND_DONE)
    }

    private fun statement(): Boolean {
        return if (match(LEFT_BRACE)) {
            beginScope()
            block(false)
            endScope()
            false
        } else if (match(IF)) {
            ifSomething(false)
            false
        } else if (match(WHEN)) {
            whenSomething(false)
            false
        } else if (match(FOR)) {
            forStatement()
            false
        } else if (match(PRINT)) {
            printStatement()
            false
        } else if (match(TokenType.RETURN)) {
            returnStatement()
            true
        } else if (match(TokenType.YIELD)) {
            yieldStatement()
            true
        } else if (match(DO)) {
            doSomething()
            false
        } else if (match(WHILE)) {
            whileStatement()
            false
        } else if (match(BREAK)) {
            breakStatement()
            true
        } else if (match(CONTINUE)) {
            continueStatement()
            true
        }
//        if (match(TokenType.YIELD)) {
//            return yieldStatement(lambda)
//        }
        else {
            return expressionStatement()
        }
    }

    private fun block(isExpr: Boolean) {
        var lastLineIsExpr = false
        while (!check(RIGHT_BRACE)) {
            matchAllNewlines()
            lastLineIsExpr = declaration()
            matchAllNewlines()
        }
        consume(RIGHT_BRACE, "Expect '}' after block!")
        if (isExpr && !lastLineIsExpr) {
            throw error(previous, "Block is not a valid expression block!")
        }
    }

    private fun ifSomething(isExpr: Boolean) {
        val opener = previous
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
            val isRealExpr = expressionOrExpressionBlock { expression() } // Assignments can be statements, we can't know till we parse
            if (!isRealExpr) {
                throw error(opener, "An if expression must have a return value.")
            }
        } else {
            statement()
        }
    }

    /**
     * @param expressionFun The block that parses the containing expression. This is necessary as we don't
     * know how deep we are in the precedence hierarchy
     */
    private fun expressionOrExpressionBlock(localVarsToBind: List<String> = emptyList(),
                                            expressionFun: () -> Boolean): Boolean {
        return if (match(LEFT_BRACE)) {
            beginScope()
            for (name in localVarsToBind) {
                declareLocal(name)
            }
            block(true)
            val popCount = endScope(false)
            val rolledBackChunk = rollBackAndSaveLastChunk()
            emitPopUnder(popCount)
            if (rolledBackChunk.chunk.opCode != POP) {
                pushLastChunk(rolledBackChunk.chunk)
                if (rolledBackChunk.chunk.opCode == JUMP) {
                    rolledBackChunk.chunk.data[0] = byteCode.size + 1 // + 1 because the opcode will go first
                }
                byteCode.addAll(rolledBackChunk.data)
            }
            true
        } else {
            for (name in localVarsToBind) {
                emitCode(POP) // Pop any leftover vars that weren't bound because we don't have a block
            }
            expressionFun()
        }
    }

    /**
     * Actually creates a list of tokens that represent if-else-ifs that are then compiled internally
     */
    private fun whenSomething(isExpr: Boolean) {
        val origin = previous
        val whenTokens = mutableListOf<Token>()
        var usesTempVar = false
        val id = if (peekSequence(IDENTIFIER, LEFT_BRACE, NEWLINE)) {
            advance()
        } else {
            usesTempVar = true
            val tempId = Token.named(nextImplicitVarName("when"))
            whenTokens += listOf(tempId, Token.ofType(EQUAL))
            whenTokens += consumeUntilType(LEFT_BRACE)
            whenTokens += Token.ofType(NEWLINE)
            tempId
        }
        var first = true
        var wroteElse = false
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
                whenTokens += previous
                whenTokens += consumeNextBlock(isExpr)
            } else if (wroteElse) {
                // We could just break when we encountered a break, but that's make for a lousy compiler
                throw error(previous, "'else' must be the last clause in a 'when' block")
            } else {
                var ifToken: Token? = null
                do {
                    val op = if (match(IS, ISNOT, IN, NOTIN, IF)) {
                        previous
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
                        whenTokens += previous
                    }
                } while (!conditionAtEndBlock())

                // Consume the statement
                whenTokens += consumeNextBlock(isExpr)
            }
            matchAllNewlines()
        }
        consume(RIGHT_BRACE, "Expect '}' at the end of when")
        if (isExpr && usesTempVar) {
            whenTokens += Token.ofType(EOF)
            compileAsCalledLambdaWithSingleReturn(whenTokens, 1) {
                compileInternal(true)
            }
        } else {
            compileNested(whenTokens, isExpr)
        }
    }

    private fun forStatement() {
        val opener = previous
        val assignmentTokens = mutableListOf<Token>()
        val ids = if (match(IDENTIFIER)) {
            assignmentTokens += previous
            listOf(previous)
        } else if (match(LEFT_BRACKET)) {
            val start = current - 1
            scanForObjectDecomp(false)?.let {
                assignmentTokens += tokens.subList(start, current)
                it.map { id -> Token.named(id) }
            } ?: throw error(previous, "Expect object decomposition in for loop.")
        } else {
            throw error(previous, "Expect identifier or object decomp after 'for'.")
        }
        consume(IN, "Expect 'in' after identifier in 'for'.")
        val iterableTokens = consumeUntilType(LEFT_BRACE)
        if (peek.type != LEFT_BRACE) {
            throw error(opener, "A for loop must include a block.")
        }
        val blockTokens = consumeNextBlock(false)
        var elseBlockTokens: List<Token>? = null
        if (match(ELSE)) {
            if (peek.type != LEFT_BRACE) {
                throw error(peek, "The for loop else must include a block.")
            }
            elseBlockTokens = consumeNextBlock(false)
        }
        val tokens = mutableListOf<Token>().apply {
            val iterator = Token.named(nextImplicitVarName("iterator"))
            val eq = Token.ofType(EQUAL)
            val lp = Token.ofType(LEFT_PAREN)
            val rp = Token.ofType(RIGHT_PAREN)
            val dot = Token.ofType(DOT)
            val nl = Token.ofType(NEWLINE)
            val ifToken = Token.ofType(IF)
            val eqEq = Token.ofType(EQUAL_EQUAL)
            val nil = Token.ofType(NIL)
            val checkTokens = mutableListOf<Token>()
            for ((i, id) in ids.withIndex()) {
                if (i > 0) {
                    checkTokens.add(Token.ofType(AND))
                }
                checkTokens.addAll(listOf(id, eqEq, nil))
            }
            addAll(listOf(iterator, eq, lp)); addAll(iterableTokens); addAll(listOf(rp, dot,
                Token.named(Constants.ITERATE), lp, rp, nl))
            if (elseBlockTokens != null) {
                addAll(listOf(ifToken, iterator, eqEq, nil))
                addAll(elseBlockTokens)
                add(Token.ofType(ELSE))
            }
            addAll(listOf(Token.ofType(WHILE), iterator))
            for ((i, blockToken) in blockTokens.withIndex()) {
                add(blockToken)
                if (i == 0) {
                    add(nl)
                    addAll(assignmentTokens)
                    addAll(listOf(eq, iterator, dot, Token.named(Constants.NEXT), lp, rp, nl, ifToken))
                    addAll(checkTokens)
                    add(Token.ofType(BREAK))
                    add(nl)
                }
            }
        }
        compileNested(tokens, false)
        // Remove the iterator as it isn't needed anymore
        discardLastLocal(true)
    }

    private fun printStatement() {
        expression()
        checkIfUnidentified()
        emitCode(OpCode.PRINT)
    }

    private fun returnStatement() {
        if (kind == Parser.KIND_INIT) {
            throw error(previous, "Can't return from init!")
        }
        consumeReturnValue(false)
        emitReturn(false) { }
        // After return is emitted, we need to close the scope, so we emit the number of additional
        // instructions to interpret before we actually return from call frame
        val pos = byteCode.size
        byteCode.putInt(0)
        val popCount = discardLocals(scopeDepth, emitPops = true, keepLocals = true)
        byteCode.setInt(popCount, pos)
    }

    private fun yieldStatement() {
        // TODO add check if we're in top-level fiber
        consumeReturnValue(true)
        emitCode(OpCode.YIELD)
    }

    private fun doSomething() {
        // First we need to determine if it's a do, do-else or do-while
        val curr = current
        if (peek.type == LEFT_BRACE) {
            val tokens = consumeNextBlock(false)
            if (match(WHILE)) { // TODO improve performance by reusing the scanned tokens above
                current = curr
                doWhileStatement()
            } else {
                doBlock(tokens)
            }
        }
    }

    private fun doWhileStatement() {
        val skipPop = emitJump(JUMP)
        val start = byteCode.size
        emitCode(POP)
        patchJump(skipPop)
        loops.push(ActiveLoop(start, scopeDepth))
        statement()
        consume(WHILE, "Expect 'while' after the statement in a do-while.")
        expression()
        emitCode(INVERT) // Cuz we're lazy and don't want to implement JUMP_IF_TRUE
        emitJump(JUMP_IF_FALSE, start)
        emitCode(POP)
        val end = byteCode.size
        val breaksToPatch = loops.pop().breaks
        for (pos in breaksToPatch) {
            pos.data[1] = end
            byteCode.setInt(end, pos.data[0] as Int)
        }
    }

    private fun doBlock(tokens: List<Token>?) {
        loops.push(DoBlock(scopeDepth))
        val isExpr = tokens == null
        if (isExpr) {
            expressionOrExpressionBlock { expression() }
        } else {
            compileNested(tokens!!, false)
        }
        val breakJumpLoc: Int
        if (match(ELSE)) {
            consume(LEFT_BRACE, "Expect '{' to start else clause of do-else block.")
            val elseSkip = emitJump(JUMP)
            emitCode(OpCode.NIL) // need to push this to have something to pop below if no breaks were reached
            breakJumpLoc = byteCode.size
            if (isExpr) {
                current-- // roll back to include the { for the expressionOrExpressionBlock call
                expressionOrExpressionBlock(listOf(Constants.IT)) { expression() }
            } else {
                beginScope()
                declareLocal(Constants.IT)
                block(isExpr)
                endScope(true)
            }
            patchJump(elseSkip)
        } else {
            breakJumpLoc = byteCode.size
        }
        val breaksToPatch = loops.pop().breaks
        for (pos in breaksToPatch) {
            pos.data[1] = breakJumpLoc
            byteCode.setInt(breakJumpLoc, pos.data[0] as Int)
        }
    }

    private fun whileStatement() {
        val start = byteCode.size
        loops.push(ActiveLoop(start, scopeDepth))
        expression()
        val skipChunk = emitJump(JUMP_IF_FALSE)
        emitCode(POP)
        statement()
        emitJump(JUMP, start)
        val end = patchJump(skipChunk)
        val breaksToPatch = loops.pop().breaks
        // Set to jump 1 after end to skip the final POP as it already happened in the loop body
        val skip = end + 1
        for (pos in breaksToPatch) {
            pos.data[1] = skip
            byteCode.setInt(skip, pos.data[0] as Int)
        }
        emitCode(POP)
    }

    private fun breakStatement() {
        if (loops.isEmpty()) {
            throw error(previous, "Cannot 'break' outside of a loop or do block!")
        }
        val activeLoop = loops.peek()
        if (activeLoop is DoBlock) { // do block breaks can return values
            consumeReturnValue(true)
            val popCount = locals.count { it.depth >= activeLoop.depth + 1 }
            emitPopUnder(popCount)
        } else {
            // Discards scopes up to the loop level (hence + 1)
            for (depth in scopeDepth downTo (activeLoop.depth + 1)) {
                discardLocals(depth, emitPops = true, keepLocals = true)
            }
        }
        val chunk = emitJump(JUMP)
        activeLoop.breaks += chunk
    }

    private fun continueStatement() {
        if (loops.isEmpty()) {
            throw error(previous, "Cannot 'continue' outside of a loop!")
        }
        val activeLoop = loops.peek()
        if (activeLoop is DoBlock) {
            // TODO this can be improved so that we look up the nearest loop and continue it, but
            // the whole thing might be confusing from the user's perspective.
            throw error(previous, "Cannot 'continue' in a 'do' block!")
        }
        // Discards scopes up to the level of loop statement (hence + 2)
        for (depth in scopeDepth downTo (activeLoop.depth + 2)) {
            discardLocals(depth, emitPops = true, keepLocals = true)
        }
        emitJump(JUMP, loops.peek().start)
    }

    private fun expressionStatement(): Boolean {
        return try {
            val shouldPop = expression()
            if (shouldPop) {
                emitCode(POP)
            }
            shouldPop
        } catch (e: StatementDeepDown) {
            false
        }
    }

    private fun expression(): Boolean {
        return rescue()
    }

    private fun rescue(): Boolean {
        val chunkScanStart = chunks.size
        val shouldPop = assignment()
        if (match(QUESTION_BANG)) {
            val chunkScanEnd = chunks.size
            val elseChunk = emitJump(JUMP_IF_EXCEPTION)
            if (shouldPop) {
                emitCode(POP) // pop the expr value since rescue always returns false, meaning expressionStatement won't pop
            }
            val endChunk = emitJump(JUMP)
            patchJump(elseChunk)
            for (i in chunkScanStart until chunkScanEnd) {
                val chunk = chunks[i]
                // All jump if exception chunks were emitted following calls/invokes, and need to be patched to go
                // to this specific place. Otherwise, they remain dormant with jump location of -1.
                if (chunk.opCode == JUMP_IF_EXCEPTION) {
                    patchJump(chunk)
                }
            }
            if (!match(LEFT_BRACE)) {
                throw error(previous, "Expect '{' to start rescue block.")
            }
            beginScope()
            declareLocal(Constants.IT)
            if (!shouldPop) {
                emitCode(DUPLICATE) // copy the value at the top of the stack, last assigned
            }
            block(false)
            endScope()
            patchJump(endChunk)
            return false
        } else {
            return shouldPop
        }
    }

    private fun assignment(): Boolean {
        firstNonAssignment(false) // left-hand side
        if (match(EQUAL, UNDERSCORE_EQUAL, DOLLAR_EQUAL, PLUS_EQUAL, MINUS_EQUAL, STAR_EQUAL,
                        SLASH_EQUAL, SLASH_SLASH_EQUAL, MOD_EQUAL, QUESTION_QUESTION_EQUAL)) {
            val equals = previous
            if (lastChunk?.opCode == GET_SUPER) {
                throw error(equals, "Setters aren't allowed with 'super'.")
            }
            if (equals.type == EQUAL || equals.type == UNDERSCORE_EQUAL) {
                val isConst = (equals.type == UNDERSCORE_EQUAL)
                if (lastChunk?.opCode == GET_PROP) {
                    /* What happens here is as follows:
                    1. Remove the GET_PROP chunk as it'll get replaced with SET_PROP down the line.
                    2. If we're in an INIT and we're looking at setting a new var (CONST_ID), store that into declaredField.
                    3. Emit the value to set to.
                    4. Add the declaredField (if it exists) to declaredFields of enclosing compiler (as its the one that's compiling the class). The reason why that's done here is to prevent false self. markings in the setting expression, e.g, @from = from would wrongly be identified as @from = @from if we added declaredField to declaredFields immediately.
                     */
                    rollBackLastChunk()
                    val declaredField = if (lastChunk?.opCode == CONST_ID && kind == Parser.KIND_INIT) {
                        lastChunk!!.data[1] as String
                    } else {
                        null
                    }
                    firstNonAssignment(true)
                    declaredField?.let { enclosing?.currentClass?.declaredFields?.add(it) }
                    emitCode(SET_PROP)
                } else {
                    if (lastChunk?.opCode == GET_UPVALUE) {
                        val name = lastChunk!!.data[0] as String
                        throw error(previous, "Name shadowed: $name.")
//                        declareLocal(name, isConst)
//                        rollBackLastChunk()
                    } else if (lastChunk?.opCode == GET_LOCAL) {
                        throw error(equals, "Variable redeclared!")
                    } else if (lastChunk?.opCode != CONST_ID) {
                        throw error(equals, "Expect an ID for var declaration!")
                    } else {
                        declareLocal(lastChunk!!.data[1] as String, isConst)
                        rollBackLastChunk()
                    }
                    firstNonAssignment(true) // push the value on the stack
                }
            } else {
                if (lastChunk?.opCode == GET_PROP) {
                    rollBackLastChunk() // this isn't a get but an update, roll back
                    val op = when (equals.type) {
                        DOLLAR_EQUAL -> throw error(equals, "Don't use $= with setters, = works just fine.")
                        QUESTION_QUESTION_EQUAL -> throw error(equals, "??= doesn't work with setters, sorry. Use @prop = prop ?? smth instead.")
                        else -> opCodeForCompoundAssignment(equals.type)
                    }
                    firstNonAssignment(true) // right-hand side
                    emitUpdateProp(op)
                } else {
                    val variable: Any = when (lastChunk?.opCode) {
                        GET_LOCAL -> lastChunk!!.data[1] as Local
                        GET_UPVALUE -> lastChunk!!.data[1] as Int
                        else -> throw error(equals, "Assigning to undeclared var!")
                    }
                    val isConst = if (variable is Local) variable.isConst else upvalues[variable as Int].isConst
                    if (isConst) {
                        throw error(previous, "Can't assign to a const!")
                    }
                    if (equals.type == QUESTION_QUESTION_EQUAL) {
                        nilCoalescenceWithKnownLeft { firstNonAssignment(true) }
                    } else {
                        if (equals.type == DOLLAR_EQUAL) {
                            rollBackLastChunk() // It'll just be a set, set-assigns reuse the already emitted GET_LOCAL
                        }
                        firstNonAssignment(true) // right-hand side
                        if (equals.type != DOLLAR_EQUAL) {
                            emitCode(opCodeForCompoundAssignment(equals.type))
                        }
                    }
                    emitSet(variable)
                }
            }
            return false
        }
        return true
    }

    // irsoa = is right side of assignment
    private fun firstNonAssignment(irsoa: Boolean) { // first expression type that's not an assignment
        or(irsoa)
    }

    private fun or(irsoa: Boolean) {
        and(irsoa)
        while (match(OR)) {
            val elseChunk = emitJump(JUMP_IF_FALSE)
            val endChunk = emitJump(JUMP)
            patchJump(elseChunk)
            emitCode(POP)
            and(irsoa)
            patchJump(endChunk)
        }
    }

    private fun and(irsoa: Boolean) {
        equality(irsoa)
        while (match(AND)) {
            val endChunk = emitJump(JUMP_IF_FALSE)
            emitCode(POP)
            equality(irsoa)
            patchJump(endChunk)
        }
    }

    private fun equality(irsoa: Boolean) {
        comparison(irsoa)
        while (match(BANG_EQUAL, EQUAL_EQUAL, IS, ISNOT, IN, NOTIN)) {
            val operator = previous
            comparison(irsoa)
            when (operator.type) {
                EQUAL_EQUAL -> emitCode(EQ)
                BANG_EQUAL -> {
                    emitCode(EQ)
                    emitCode(INVERT)
                }
                IS -> emitCode(OpCode.IS)
                ISNOT -> {
                    emitCode(OpCode.IS)
                    emitCode(INVERT)
                }
                IN -> emitCode(HAS)
                NOTIN -> {
                    emitCode(HAS)
                    emitCode(INVERT)
                }
                else -> throw IllegalArgumentException("WTF")
            }
        }
    }

    private fun comparison(irsoa: Boolean) {
        range(irsoa)
        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL, LESS_GREATER)) {
            val operator = previous
            range(irsoa)
            if (operator.type == LESS_GREATER) {
                emitInvoke(Constants.MATCHES, 1)
            } else {
                emitCode(when (operator.type) {
                    GREATER -> GT
                    GREATER_EQUAL -> GE
                    LESS -> LT
                    LESS_EQUAL -> LE
                    else -> throw IllegalArgumentException("WTF")
                })
            }
        }
    }

    private fun range(irsoa: Boolean) {
        addition(irsoa)
        while (match(DOT_DOT, DOT_DOT_DOT)) {
            val operator = previous
            addition(irsoa)
            emitInvoke(when (operator.type) {
                DOT_DOT -> "rangeUntil"
                DOT_DOT_DOT -> "rangeTo"
                else -> throw IllegalArgumentException("WTF")
            }, 1)
        }
    }

    private fun addition(irsoa: Boolean) {
        multiplication(irsoa)
        while (match(MINUS, PLUS)) {
            val operator = previous
            multiplication(irsoa)
            emitCode(if (operator.type == PLUS) ADD else SUBTRACT)
        }
    }

    private fun multiplication(irsoa: Boolean) {
        nilCoalescence(irsoa)
        while (match(SLASH, SLASH_SLASH, STAR, MOD)) {
            val operator = previous
            nilCoalescence(irsoa)
            emitCode(when (operator.type) {
                SLASH -> DIVIDE
                SLASH_SLASH -> DIVIDE_INT
                STAR -> MULTIPLY
                MOD -> OpCode.MOD
                else -> throw IllegalArgumentException("WTF")
            })
        }
    }

    private fun nilCoalescence(irsoa: Boolean) {
        unary(irsoa)
        while (match(QUESTION_QUESTION)) {
            nilCoalescenceWithKnownLeft { unary(irsoa) }
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

    private fun unary(irsoa: Boolean) {
        if (match(NOT, MINUS, /*STAR,*/ TokenType.GU)) {
            val operator = previous
            unary(irsoa)
            if (operator.type == STAR) {
                if (totalCallArgumentCounter == 0) {
                    throw error(operator, "Can't use the spread operator * outside a call!")
                }
                emitCode(SPREAD)
            } else {
                emitCode(when (operator.type) {
                    NOT -> INVERT
                    MINUS -> NEGATE
                    TokenType.GU -> OpCode.GU
                    else -> throw IllegalArgumentException("WTF")
                })
            }
        }
        /*return if (match(IVIC)) {
            Ivic(unary())
        } */
        else {
            call(irsoa)
        }
    }

    private fun call(irsoa: Boolean) {
        primary(irsoa, true)
        if (match(BANG)) { // ! is at same precedence level as calls as opposed to being unary
            // also, sequential ! calls don't make sense, but following it with a getter does
            emitCode(GET_ANNOTATIONS)
        }
        // Every iteration below decrements first, so it needs to start at 2 to be > 0 for the first check
        var superCount = if (lastChunk?.opCode == OpCode.SUPER) 2 else 1
        while (true) {
            superCount--
            val wasSuper = superCount > 0
            if (match(LEFT_PAREN, QUESTION_LEFT_PAREN)) {
                if (previous.type == QUESTION_LEFT_PAREN) {
                    emitCode(NIL_CHECK)
                }
                finishCall()
            } else if (match(DOT, QUESTION_DOT)) {
                val wasSafeGet = previous.type == QUESTION_DOT
                if (wasSafeGet) {
                    emitCode(NIL_CHECK)
                }
                if (match(CLASS)) {
                    emitId(Constants.CLASS)
                } else if (match(LEFT_PAREN)) {
                    expression()
                    consume(RIGHT_PAREN, "Expect ')' after evaluated getter.")
                    finishGet(wasSuper)
                    continue // Go the the next iteration to prevent INVOKE shenanigans
                } else {
                    primary(irsoa = false, checkImplicitSelf = false)
                }

                if (!wasSafeGet && match(LEFT_PAREN)) { // Check invoke, currently disabled for safe getter calls
                    val name = lastChunk!!.data[if (lastChunk?.opCode == GET_LOCAL) 0 else 1] as String
                    rollBackLastChunk()
                    val argCount = callArgList()
                    emitInvoke(name, argCount, wasSuper)
                } else {
                    if (lastChunk?.opCode == GET_LOCAL || lastChunk?.opCode == GET_UPVALUE) {
                        val name = lastChunk!!.data[0] as String
                        rollBackLastChunk()
                        emitId(name)
                    }
                    finishGet(wasSuper)
                }
            } else {
                break
            }
        }
    }

    private fun finishCall() {
        checkIfUnidentified()
        val argCount = callArgList()
        emitCall(argCount)
    }

    private fun callArgList(): Int {
        callArgumentsCounter++
        var count = 0
        if (!check(RIGHT_PAREN)) {
            do {
                count++
                matchSequence(IDENTIFIER, EQUAL) // allows for named params, e.g substr(start=1,end=2)
                expression()
            } while (match(COMMA))
        }
        consume(RIGHT_PAREN, "Expect ')' after arguments.")
        callArgumentsCounter--
        return count
    }

    private fun finishGet(wasSuper: Boolean) {
        if (wasSuper) {
            if (lastChunk?.opCode != CONST_ID) {
                throw error(previous, "'super' get can only involve an identifier.")
            }
            emitCode(GET_SUPER)
        } else {
            emitCode(GET_PROP)
        }
    }

    private fun primary(irsoa: Boolean, checkImplicitSelf: Boolean) {
        if (match(TokenType.FALSE)) {
            emitCode(OpCode.FALSE)
        } else if (match(TokenType.TRUE)) {
            emitCode(OpCode.TRUE)
        } else if (match(NIL)) {
            emitCode(OpCode.NIL)
        } else if (match(NUMBER, STRING)) {
            val token = previous
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
        } else if (match(TokenType.SUPER)) {
            if (currentClass == null) {
                throw error(previous, "Cannot use 'super' outside of class.")
            } else if (currentClass?.firstSuperclass == null) {
                throw error(previous, "Cannot use 'super' in a class that doesn't have a superclass.")
            }
            val superclass = if (match(LEFT_PAREN)) {
                consumeVar("Expect superclass name in parentheses.")
            } else {
                currentClass?.firstSuperclass!!
            }
            emitSuper(superclass)
        } else if (match(SELF)) {
            if (peekSequence(LEFT_PAREN, FN, RIGHT_PAREN)) {
                emitCode(SELF_FN)
                advance(); advance(); advance()
            } else {
                if (currentClass == null) {
                    throw error(previous, "Cannot use 'self' outside of class.")
                }
                variable()
            }
        } else if (match(LEFT_BRACKET)) {
            if (irsoa) { // object decomp can't be on the left-hand side
                objectLiteralOrObjectComprehension()
            } else {
                scanForObjectDecomp(true)?.let {
                    objectDecomp(it)
                } ?: run {
                    objectLiteralOrObjectComprehension()
                }
            }
        } else if (match(DOLLAR_LEFT_BRACKET)) {
            objectLiteralOrObjectComprehension()
        } else if (match(FN) || peekSequence(EQUAL)) {
            function(Parser.KIND_LAMBDA, false, nextLambdaName())
        } else if (match(DO)) {
            proc()
        } else if (match(IDENTIFIER)) {
            val name = previous.lexeme
            if (checkImplicitSelf && validateImplicitSelf(currentClass, name)) {
                variable(Constants.SELF)
                emitId(name)
                finishGet(false)
            } else {
                variable(name)
                if (irsoa) {
                    checkIfUnidentified()
                }
            }
        } else if (match(LEFT_PAREN)) {
            expression()
            consume(RIGHT_PAREN, "Expect ')' after expression.")
        } else if (match(IF)) {
            ifSomething(true)
        } else if (match(WHEN)) {
            whenSomething(true)
        } else {
            throw error(peek, "Expect expression.")
        }
    }

    private fun variable(providedName: String? = null) {
        val name = providedName ?: previous.lexeme
        val local = findLocal(this, name)
        if (local != null) {
            emitGetLocal(name, local)
        } else {
            val upvalue = resolveUpvalue(this, name)?.first
            if (upvalue != null) {
                emitGetUpvalue(name, upvalue)
            } else {
                emitId(name)
            }
        }
    }

    private fun proc() {
        val skipChunk = emitJump(JUMP) // unconditional jump to skip over the proc body during declaration
        val start = byteCode.size
        doBlock(null)
        val end = byteCode.size
        patchJump(skipChunk)
        emitProc(start, end)
    }

    private fun objectLiteralOrObjectComprehension() {
        val opener = previous
        if (match(FOR)) {
            objectComprehension(opener)
        } else {
            objectLiteral(opener)
        }
    }

    /**
     * The algorithm is as follows:
     *  1. Take the for part of the comprehension as-is. Add a { and newline after it.
     *  2. Check if there's an if part. If so, add it as-is and append { and newline.
     *  3. Check if there's an = in the tokens between DO and ].
     *  4. Repeat for both sides of the =, or just once if there's no =: Emit the tokens as a statement, add a newline,
     *      then insert a #addToComprehension internal compiler directive.
     *  5. Close the if (if it exists) and the for with } and newlines.
     *  6. Compile the synthesized for internally.
     */
    private fun objectComprehension(opener: Token) {
        var isList = true
        current-- // include the skipped for
        val tokens = mutableListOf<Token>()
        val nl = Token.ofType(NEWLINE)
        val lbnl = listOf(Token.ofType(LEFT_BRACE), nl)
        val rbnl = listOf(Token.ofType(RIGHT_BRACE), nl)
        val nlHashAddToComprehension = listOf(nl, Token.ofType(HASH), Token.named(ADD_TO_COMPREHENSION))
        var hasIf = false
        tokens += consumeUntilType(IF, DO)
        tokens += lbnl
        if (peek.type == IF) {
            hasIf = true
            tokens += advance() // add the IF
            tokens += consumeUntilType(DO)
            tokens += lbnl
        }
        advance() // skip the DO, it's just there to make the syntax nicer
        var restOfTokens = consumeUntilType(RIGHT_BRACKET)
        val equalityIndex = restOfTokens.indexOfFirst { it.type == EQUAL }
        if (equalityIndex != -1) {
            isList = false
            tokens += restOfTokens.subList(0, equalityIndex)
            tokens += nlHashAddToComprehension
            restOfTokens = restOfTokens.subList(equalityIndex + 1, restOfTokens.size)
        }
        tokens += restOfTokens
        tokens += nlHashAddToComprehension
        if (hasIf) {
            tokens += rbnl // close the if block
        }
        tokens += rbnl // close the for block
        tokens += Token.ofType(EOF)
        consume(RIGHT_BRACKET, "Expect ']' at the end of object comprehension.")
        compileAsCalledLambdaWithSingleReturn(tokens, 0) {
            declareLocal(nextImplicitVarName("objCompr"), true)
            emitCode(START_COMPREHENSION)
            compileInternal(false)
            emitObjectLiteral(opener, isList, -1)
        }
    }

    private fun objectLiteral(opener: Token) {
        var count = 0
        var isList: Boolean? = null
        if (!check(RIGHT_BRACKET)) {
            matchAllNewlines()
            do {
                matchAllNewlines()
                if (isList == null) { // we still need to determine if it's a list or an object
                    isList = !(peekSequence(IDENTIFIER, EQUAL) || peekSequence(STRING, EQUAL))
                }
                if (isList) {
                    expression()
                } else {
                    emitConst(if (match(STRING)) {
                        previous.literal.string.toString()
                    } else {
                       consumeVar("Expect identifier or string as object key.")
                    })
                    consume(EQUAL, "Expect '=' between object key and value pair.")
                    expression()
                }
                matchAllNewlines()
                count++
            } while (match(COMMA))
            matchAllNewlines()
        }
        consume(RIGHT_BRACKET, "Expect ']' at the end of object.")
        emitObjectLiteral(opener, isList == true, count)
    }

    /**
     * @return list of identifiers found in the decomp, or null if it isn't a valid decomp
     */
    private fun scanForObjectDecomp(needsEquals: Boolean): List<String>? {
        if (peek.type == RIGHT_BRACKET) {
            return null // object decomp can't be empty
        }
        val ids = mutableListOf<String>()
        val curr = current
        do {
            if (match(IDENTIFIER)) {
                ids += previous.lexeme
            } else {
                current = curr
                return null
            }
        } while (match(COMMA))
        return if (match(RIGHT_BRACKET)) {
            if (needsEquals && match(EQUAL) || !needsEquals) {
                ids
            } else {
                current = curr
                null
            }
        } else {
            current = curr
            null
        }
    }

    private fun objectDecomp(ids: List<String>) {
        val tempVarName = nextImplicitVarName("objDecomp")
        declareLocal(tempVarName)
        firstNonAssignment(true)
        consume(NEWLINE, "Expect newline after object decomposition.")
        val tokens = mutableListOf<Token>()
        val eq = Token.ofType(EQUAL)
        val tempVarToken = Token.named(tempVarName)
        val dot = Token.ofType(DOT)
        val nilCoal = Token.ofType(QUESTION_QUESTION)
        val nl = Token.ofType(NEWLINE)
        for ((i, id) in ids.withIndex()) {
            val idToken = Token.named(id)
            tokens.addAll(listOf(idToken, eq, tempVarToken, dot, idToken, nilCoal, tempVarToken, dot, Token.ofLong(i.toLong()), nl))
        }
        compileNested(tokens, false)
        throw StatementDeepDown() // unwind all the way to the top and prevent emitting of expressionStatement POP
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

    private fun consumeArgList(): List<Token> {
        val start = current
        var parenCount = 0
        var end = 0
        loop@ for (i in start until tokens.size) {
            when (tokens[i].type) {
                LEFT_PAREN -> parenCount++
                RIGHT_PAREN -> {
                    parenCount--
                    if (parenCount == 0) {
                        end = i
                        break@loop
                    }
                }
            }
        }
        current = end + 1
        val sublist = tokens.subList(start, current)
        return sublist
    }

    private fun scanNextExpr(): List<Token> {
        val start = current
        var end = 0
        var parenCount = 0
        var bracketCount = 0
        loop@ for (i in start until tokens.size) {
            when (tokens[i].type) {
                NEWLINE, RIGHT_BRACE, EOF -> {
                    end = i
                    break@loop
                }
                LEFT_PAREN -> parenCount++
                LEFT_BRACKET -> bracketCount++
                RIGHT_PAREN -> {
                    parenCount--
                    if (parenCount < 0) {
                        end = i
                        break@loop
                    }
                }
                RIGHT_BRACKET -> {
                    bracketCount--
                    if (bracketCount < 0) {
                        end = i
                        break@loop
                    }
                }
                COMMA -> {
                    if (parenCount == 0 && bracketCount == 0) {
                        end = i
                        break@loop
                    }
                }
            }
        }
        return tokens.subList(start, end)
    }

    private fun consumeNextExpr(): List<Token> {
        val tokens = scanNextExpr()
        current += tokens.size
        return tokens
    }

    private fun consumeUntilType(vararg types: TokenType): List<Token> {
        val start = current
        while (!isAtEnd() && !check(*types)) {
            if (peek.type == NEWLINE) {
                throw error(peek, "Unreachable token types: ${types.joinToString()}.")
            }
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

    private fun matchClassOpener() = match(MODULE, ENUM, CLASS, CLASS_FINAL, CLASS_OPEN)

    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()
        throw error(peek, message)
    }

    private fun consumeVar(message: String): String {
        return consume(IDENTIFIER, message).lexeme
    }

    private fun consumeValue(message: String): Any {
        return when {
            match(TokenType.FALSE) -> false
            match(TokenType.TRUE) -> true
            match(NIL) -> Nil
            match(NUMBER) -> {
                val num = previous.literal.number
                if (num.isInteger) {
                    num.asLong()
                } else {
                    num.asDouble()
                }
            }
            match(STRING) -> previous.literal.string
            else -> throw error(peek, message)
        }
    }

    private fun consumeReturnValue(emitNil: Boolean) {
        if (match(NEWLINE)) {
            if (emitNil) {
                emitCode(OpCode.NIL)
            }
        } else {
            expression()
            checkIfUnidentified()
            if (peek.type != RIGHT_BRACE) {
                consume(NEWLINE, "Expect newline after return value.")
            }
        }
    }

    private fun consumeDynamicTypeCheck(message: String): DynamicTypeCheck {
        val type = consumeVar(message)
        var canNil = false
        var canException = false
        if (match(QUESTION)) {
            canNil = true
        } else if (match(BANG)) {
            canException = true
        }
        return DynamicTypeCheck(type, canNil, canException)
    }

    /**
     * Wraps the prepared tokens in a lambda that returns a single value, and is immediately called.
     * Used for when expressions and object comprehensions because they have sideeffect vars that can't
     * live in the same scope as the expression they're transpiled from.
     */
    private fun compileAsCalledLambdaWithSingleReturn(tokens: List<Token>, popsAfterReturn: Int, block: Compiler.() -> Unit) {
        val compiler = Compiler().apply {
            enclosing = this@Compiler
        }
        val f = compiler.compileFunction(tokens, nextLambdaName(), 0, Parser.KIND_LAMBDA) {
            this.block()
            emitCode(OpCode.RETURN)
            byteCode.putInt(popsAfterReturn)
            for (i in 0 until popsAfterReturn) {
                emitCode(POP)
            }
        }
        emitClosure(f, compiler, false)
        emitCall(0)
    }

    private fun check(vararg tokenTypes: TokenType): Boolean {
        for (tokenType in tokenTypes) {
            if (isAtEnd() && tokenType == EOF) {
                return true
            }
            if (peek.type == tokenType) {
                return true
            }
        }
        return false
    }

    private fun advance(): Token {
        if (!isAtEnd()) current++
        return previous
    }

    private fun isAtEnd(): Boolean {
        return current == tokens.size || peek.type == EOF
    }

    private val peek: Token get() = tokens[current]

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

    private val previous: Token get() = tokens[current - 1]

    private fun rollBack(by: Int = 1) {
        for (i in 0 until by) {
            byteCode.removeAt(byteCode.size - 1)
        }
    }

    private fun rollBackLastChunk() {
        rollBack(lastChunk!!.size)
        chunks.removeAt(chunks.size - 1)
        lastChunk = if (chunks.isEmpty()) null else chunks.last()
    }

    private fun rollBackAndSaveLastChunk(): RolledBackChunk {
        val chunk = lastChunk!!
        val size = byteCode.size
        val data = mutableListOf<Byte>()
        for (i in size - chunk.size until size) {
            data += byteCode[i]
        }
        rollBackLastChunk()
        return RolledBackChunk(chunk, data)
    }

    private fun error(token: Token, message: String): CompileError {
        return CompileError(bundleTokenWithMessage(token, message) + TokenPatcher.patch(tokens, token))
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

    private fun emitIdOrConst(opCode: OpCode, c: Any) {
        var size = byteCode.put(opCode)
        val constIdx = constIndex(c)
        size += byteCode.putInt(constIdx)
        pushLastChunk(Chunk(opCode, size, constIdx, c))
    }

    private fun emitClosure(f: Function, funcCompiler: Compiler, isFiber: Boolean) {
        val opCode = if (isFiber) FIBER else CLOSURE
        var size = byteCode.put(opCode)
        val constIdx = constIndex(f)
        size += byteCode.putInt(constIdx)
        for (i in 0 until f.upvalueCount) {
            val upvalue = funcCompiler.upvalues[i]
            size += byteCode.put((if (upvalue.isLocal) 1 else 0).toByte())
            size += byteCode.putInt(upvalue.sp)
            size += byteCode.putInt(constIndex(upvalue.fiberName))
        }
        pushLastChunk(Chunk(opCode, size, constIdx, f))
    }

    private fun emitClass(name: String, kind: SClass.Kind, inner: Boolean) {
        val opCode = if (inner) INNER_CLASS else OpCode.CLASS
        var size = byteCode.put(opCode)
        size += byteCode.put(kind.byte)
        val constIdx = constIndex(name)
        size += byteCode.putInt(constIdx)
        pushLastChunk(Chunk(opCode, size, kind, constIdx, name))
    }

    private fun emitMethod(name: String, isExtension: Boolean) {
        val opCode = if (isExtension) EXTEND_METHOD else METHOD
        var size = byteCode.put(opCode)
        val constIdx = constIndex(name)
        size += byteCode.putInt(constIdx)
        pushLastChunk(Chunk(opCode, size, constIdx, name))
    }

    private fun emitNativeMethod(name: String, nativeFunction: NativeFunction, isExtension: Boolean) {
        val opCode = if (isExtension) EXTEND_NATIVE_METHOD else NATIVE_METHOD
        var size = byteCode.put(opCode)
        val nameIdx = constIndex(name)
        size += byteCode.putInt(nameIdx)
        val funIdx = constIndex(nativeFunction)
        size += byteCode.putInt(funIdx)
        pushLastChunk(Chunk(opCode, size, name, nameIdx, funIdx))
    }

    private fun emitField(name: String) {
        val opCode = FIELD
        var size = byteCode.put(opCode)
        val constIdx = constIndex(name)
        size += byteCode.putInt(constIdx)
        pushLastChunk(Chunk(opCode, size, constIdx, name))
    }

    private fun emitAnnotateField(name: String) {
        val opCode = ANNOTATE_FIELD
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
        pushLastChunk(Chunk(opCode, size, name, index, upvalue))
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

    private fun emitUpdateProp(op: OpCode) {
        val opCode = UPDATE_PROP
        var size = byteCode.put(opCode)
        size += byteCode.put(op)
        pushLastChunk(Chunk(opCode, size, op))
    }

    private fun emitJump(opCode: OpCode, location: Int? = null): Chunk {
        var size = byteCode.put(opCode)
        val offset = byteCode.size
        val skip = location ?: 0
        size += byteCode.putInt(skip)
        val chunk = Chunk(opCode, size, offset, skip)
        pushLastChunk(chunk)
        return chunk
    }

    private fun patchJump(chunk: Chunk): Int {
        val offset = chunk.data[0] as Int
        val skip = byteCode.size
        byteCode.setInt(skip, offset)
        chunk.data[1] = skip
        return skip
    }

    private fun emitCall(argCount: Int) {
        val opCode = CALL
        var size = byteCode.put(opCode)
        size += byteCode.putInt(argCount)
        pushLastChunk(Chunk(opCode, size, argCount))
        emitJump(JUMP_IF_EXCEPTION, CALL_DEFAULT_JUMP_LOCATION)
    }

    private fun emitInvoke(name: String, argCount: Int, wasSuper: Boolean = false) {
        val opCode = if (wasSuper) SUPER_INVOKE else INVOKE
        var size = byteCode.put(opCode)
        val constIdx = constIndex(name)
        size += byteCode.putInt(constIdx)
        size += byteCode.putInt(argCount)
        pushLastChunk(Chunk(opCode, size, name, argCount, constIdx))
        emitJump(JUMP_IF_EXCEPTION, CALL_DEFAULT_JUMP_LOCATION)
    }

    private fun emitSuper(superclass: String) {
        val opCode = OpCode.SUPER
        var size = byteCode.put(opCode)
        val constIdx = constIndex(superclass)
        size += byteCode.putInt(constIdx)
        pushLastChunk(Chunk(opCode, size, superclass, constIdx))
    }

    private fun emitReturnTypeVerification() {
        verifyReturnType?.let { type ->
            verifyReturnType = null // to prevent infinite recursion due to checks when returning TypeMismatchException
            val name = "__returnVerification_${numberOfEnclosingCompilers}_${returnVerificationVarCounter++}__"
            declareLocal(name)
            compileNested(getTypeVerificationTokens(Token.named(name), type), false)
            verifyReturnType = type // assign it back as there could be multiple returns in a body
        }
    }

    private fun emitReturn(value: () -> Unit) {
        emitReturn(true, value)
    }

    private fun emitReturn(emitSkipZero: Boolean, value: () -> Unit) {
        value()
        emitReturnTypeVerification()
        val opCode = OpCode.RETURN
        var size = byteCode.put(opCode)
        if (emitSkipZero) {
            size += byteCode.putInt(0) // No additional instructions to skip
        }
        pushLastChunk(Chunk(opCode, size))
    }

    private fun emitReturnNil() {
        emitReturn {
            emitCode(OpCode.NIL)
        }
    }

    private fun emitProc(start: Int, end: Int) {
        val opCode = PROC
        var size = byteCode.put(opCode)
        size += byteCode.putInt(start)
        size += byteCode.putInt(end)
        pushLastChunk(Chunk(opCode, size, start, end))
    }

    private fun emitObjectLiteral(opener: Token, isList: Boolean, propCount: Int) {
        val isMutable = opener.type == DOLLAR_LEFT_BRACKET
        val opCode = if (isList) LIST else OBJECT
        var size = byteCode.put(opCode)
        size += byteCode.put(if (isMutable) 1.toByte() else 0.toByte())
        size += byteCode.putInt(propCount)
        pushLastChunk(Chunk(opCode, size, isMutable, propCount))
    }

    private fun pushLastChunk(chunk: Chunk) {
        chunk.pos = byteCode.size - chunk.size
        lastChunk = chunk
        chunks += chunk
    }

    private fun updateDebugInfo(token: Token) {
        debugInfoLines.putIfAbsent(token.line, byteCode.size)
        if (token.hasBreakpoint) {
            debugInfoBreakpoints += byteCode.size
        }
    }

    private fun declareLocal(name: String, isConst: Boolean = false): Local {
        val end = locals.size
        for (i in end - 1 downTo 0) {
            val local = locals[i]
            if (local.depth < scopeDepth) {
                break
            }
            if (local.name == name) {
                throw error(previous, "Variable $name is already declared in scope!")
            }
        }
        val l = Local(name, end, scopeDepth, false)
        if (isConst) {
            l.isConst = true
        }
        locals += l
        return l
    }

    private fun findLocal(compiler: Compiler, name: String): Local? {
        for (i in compiler.locals.size - 1 downTo 0) {
            val local = compiler.locals[i]
            if (local.name == name) {
                return local
            }
        }
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

    private fun discardLastLocal(pop: Boolean) {
        locals.removeAt(locals.size - 1)
        if (pop) {
            emitCode(POP)
        }
    }

    private fun resolveUpvalue(compiler: Compiler, name: String): Pair<Upvalue, Int>? {
        if (compiler.enclosing == null) {
            return null
        }
        val enclosing = compiler.enclosing!!
        val local = findLocal(enclosing, name)
        if (local != null) {
            local.isCaptured = true
            return addUpvalue(compiler, local.sp, true, name, local.isConst, enclosing.fiberName)
        }
        val pair = resolveUpvalue(enclosing, name)
        if (pair != null) {
            return addUpvalue(compiler, pair.second, false, name, pair.first.isConst, enclosing.fiberName)
        }
        return null
    }

    private fun addUpvalue(compiler: Compiler,
                           sp: Int,
                           isLocal: Boolean,
                           name: String,
                           isConst: Boolean,
                           fiberName: String
    ): Pair<Upvalue, Int> {
        for ((index, upvalue) in compiler.upvalues.withIndex()) {
            if (upvalue.sp == sp && upvalue.isLocal == isLocal && upvalue.fiberName == fiberName) {
                return upvalue to index
            }
        }
        val upvalue = Upvalue(sp, isLocal, name, isConst, fiberName)
        compiler.upvalues += upvalue
        return upvalue to compiler.upvalues.size - 1
    }

    private fun opCodeForCompoundAssignment(type: TokenType): OpCode {
        return when (type) {
            PLUS_EQUAL -> ADD
            MINUS_EQUAL -> SUBTRACT
            STAR_EQUAL -> MULTIPLY
            SLASH_EQUAL -> DIVIDE
            SLASH_SLASH_EQUAL -> DIVIDE_INT
            MOD_EQUAL -> OpCode.MOD
            else -> throw IllegalArgumentException("WTF")
        }
    }

    private fun printChunks(name: String) {
        val tabLevel = numberOfEnclosingCompilers
        if (tabLevel > 0) {
            println("  ".repeat(tabLevel - 1) + "in $name")
        }
        val tabs = "  ".repeat(tabLevel)
        for (chunk in chunks) {
            println(tabs + chunk)
        }
    }

    private fun checkIfUnidentified() {
        if (lastChunk?.opCode == CONST_ID) {
            throw error(previous, "Unable to resolve identifier: ${lastChunk!!.data[1]}")
        }
    }

    private fun resetAnnotationCounter() {
        annotationCounter = 0
    }

    private fun checkAnnotationCounter() {
        if (annotationCounter > 0) {
            throw error(previous, "Unable to annotate this line.")
        }
    }

    private fun nextLambdaName(): String {
        return "__lambda_${numberOfEnclosingCompilers}_${lambdaCounter++}__"
    }

    private fun nextImplicitVarName(opener: String): String {
        return "__${opener}_${numberOfEnclosingCompilers}_${implicitVarCounter++}__"
    }

    private fun getTypeVerificationTokens(name: Token, type: DynamicTypeCheck): List<Token> {
        val list = mutableListOf(Token.ofType(IF), Token.ofType(LEFT_PAREN), name,
                Token.ofType(ISNOT), Token.named(type.type))
        if (type.canException) {
            list += listOf(Token.ofType(AND), name, Token.ofType(ISNOT),
                    Token.named(Constants.CLASS_EXCEPTION), Token.ofType(RIGHT_PAREN))
        } else {
            list += Token.ofType(RIGHT_PAREN)
        }
        if (!type.canNil) {
            list += listOf(Token.ofType(OR), name, Token.ofType(EQUAL_EQUAL), Token.ofType(NIL))
        }
        list += listOf(Token.ofType(TokenType.RETURN), Token.named(Constants.EXCEPTION_TYPE_MISMATCH),
                Token.ofType(LEFT_PAREN), Token.ofType(RIGHT_PAREN), Token.ofType(NEWLINE)
        ) // TODO fix
        return list
    }

    private fun scanForImplicitArgs(opener: Token, isExpr: Boolean): List<String> {
        val args = mutableListOf<String>()
        val curr = current
        val bodyTokens = if (isExpr) {
            scanNextExpr()
        } else {
            current-- // Go back to {
            consumeNextBlock(false)
        }
        current = curr
        for (token in bodyTokens) {
            if (token.type == IDENTIFIER) {
                val argName = token.lexeme
                if (IMPLICIT_ARG.matcher(argName).matches()) {
                    args += argName
                }
            }
        }
        args.sort()
        for (i in 0 until args.size) {
            val expected = "_$i"
            if (args[i] != expected) {
                throw error(opener, "Invalid implicit arg order, found ${args[i]} instead of $expected!")
            }
        }
        return args
    }

    /**
     * Searches the current class being compiled for the field name and sees if it's maybe an implicit self.
     */
    private fun validateImplicitSelf(klass: ClassCompiler?, name: String): Boolean {
        klass?.let {
            if (name in it.declaredFields) {
                return true
            }
            for (superclass in it.superclasses) {
                if (validateImplicitSelf(compiledClasses[superclass], name)) {
                    return true
                }
            }
        }
        return false
    }

    private data class Const(val index: Int,
                             val level: Int
    )

    private data class Local(val name: String,
                             val sp: Int,
                             val depth: Int,
                             var isCaptured: Boolean
    ) {
        var isConst = (name == Constants.SELF || CONST_IDENTIFIER.matcher(name).matches())
    }

    private data class Upvalue(val sp: Int,
                               val isLocal: Boolean,
                               val name: String,
                               val isConst: Boolean,
                               val fiberName: String
    )

    private open class ActiveLoop(val start: Int, val depth: Int) {
        val breaks = mutableListOf<Chunk>()
    }

    private class DoBlock(depth: Int) : ActiveLoop(-1, depth) // start is unimportant because you can't continue from do

    private class Chunk(val opCode: OpCode,
                        var size: Int,
                        val data: MutableList<Any>) {
        constructor(opCode: OpCode, size: Int, vararg data: Any) : this(opCode, size, data.toMutableList())

        var pos = 0

        override fun toString(): String {
            return "${"%5d".format(pos)}: $opCode, size: $size b, data: ${data.joinToString()}"
        }
    }

    private class ClassCompiler(val name: String,
                                var enclosing: ClassCompiler?
    ) {
        var firstSuperclass: String? = null
        var superclasses = emptyList<String>()
        var initAdditionalTokens = mutableListOf<Token>()
        val declaredFields = mutableSetOf<String>()
    }

    private data class ArgScanResult(val args: MutableList<String>,
                                     val prependedTokens: MutableList<Token>,
                                     val optionalParamsStart: Int,
                                     val defaultValues: List<Any>
    )

    private data class DynamicTypeCheck(val type: String,
                                        val canNil: Boolean,
                                        val canException: Boolean
    )

    private class RolledBackChunk(val chunk: Chunk,
                                  val data: List<Byte>
    )

    private class CompileError(message: String) : RuntimeException(message)

    private class StatementDeepDown : RuntimeException("")

    companion object {
        const val CALL_DEFAULT_JUMP_LOCATION = -1
        private const val SCRIPT = "Script"
        private val IMPLICIT_ARG = Pattern.compile("_[0-9]+")
        private val CONST_IDENTIFIER = Pattern.compile("(_)*[A-Z]+((_)*[A-Z]*)*(_)*")

        private const val ADD_TO_COMPREHENSION = "addToComprehension"
    }
}