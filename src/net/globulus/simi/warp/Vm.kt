package net.globulus.simi.warp

import net.globulus.simi.Constants
import net.globulus.simi.warp.OpCode.*
import net.globulus.simi.warp.debug.Debugger
import net.globulus.simi.warp.native.NativeFunction
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.util.*
import kotlin.math.round

class Vm {
    internal lateinit var fiber: Fiber
    private var openUpvalues = mutableMapOf<String, Upvalue>()
    private val annotationBuffer = mutableListOf<Any>()
    private var nonFinalizedClasses = Stack<Int>()

    private var debugger: Debugger? = null

    fun interpret(input: Fiber, debugMode: Boolean) {
        instance = this
        if (debugMode) {
            debugger = Debugger(this)
        }
        fiber = input
        push(input)
        try {
            await(true, 0)
        } catch (ignored: IllegalStateException) {
            val a = 5
        } // Silently abort the program
        instance = null
    }

    private fun run(breakAtFp: Int, predicate: (() -> Boolean)? = null) {
        loop@ while (predicate?.invoke() != false) {
            debugger?.triggerBreakpoint()
            val code = nextCode
            when (code) {
                TRUE -> push(true)
                FALSE -> push(false)
                CONST_INT -> push(nextLong)
                CONST_FLOAT -> push(nextDouble)
                CONST_ID -> push(nextString)
                CONST -> pushConst()
                NIL -> push(Nil)
                POP -> fiber.sp--
                POP_UNDER -> {
                    val value = pop()
                    fiber.sp -= nextInt
                    push(value)
                }
                DUPLICATE -> push(peek())
                SET_LOCAL -> setVar()
                GET_LOCAL -> getVar(nextInt)
                SET_UPVALUE -> fiber.frame.closure.upvalues[nextInt]!!.location = WeakReference(fiber.sp - 1) // -1 because we need to point at an actual slot
                GET_UPVALUE -> push(readUpvalue(fiber.frame.closure.upvalues[nextInt]!!))
                SET_PROP -> setProp()
                GET_PROP -> getProp()
                UPDATE_PROP -> updateProp()
                INVERT -> invert()
                NEGATE -> negate()
                ADD -> add()
                SUBTRACT, MULTIPLY, DIVIDE, DIVIDE_INT, MOD, LE, LT, GE, GT -> binaryOpOnStack(code)
                EQ -> checkEquality(code)
                IS -> checkIs()
                HAS -> {
                    val b = fiber.stack[fiber.sp - 1]
                    fiber.stack[fiber.sp - 1] = fiber.stack[fiber.sp - 2]
                    fiber.stack[fiber.sp - 2] = b
                    invoke(Constants.HAS, 1)
                }
                PRINT -> println(stringify(pop()))
                JUMP -> buffer.position(nextInt)
                JUMP_IF_FALSE -> jumpIf { isFalsey(it) }
                JUMP_IF_NIL -> jumpIf { it == Nil }
                JUMP_IF_EXCEPTION -> jumpIf { isException(it) }
                CALL -> {
                    val argCount = nextInt
                    call(peek(argCount), argCount)
                }
                INVOKE -> {
                    val name = nextString
                    val argCount = nextInt
                    invoke(name, argCount)
                }
                CLOSURE -> closure(false)
                FIBER -> closure(true)
                PROC -> push(Proc(nextInt, nextInt))
                CLOSE_UPVALUE -> closeUpvalue()
                RETURN -> {
                    if (doReturn(breakAtFp)) {
                        break@loop
                    }
                }
                CLASS -> declareClass(SClass.Kind.from(nextByte), nextString)
                INHERIT -> inherit()
                MIXIN, EXTEND_MIXIN -> mixin(code == EXTEND_MIXIN)
                METHOD, EXTEND_METHOD -> defineMethod(nextString, code == EXTEND_METHOD)
                NATIVE_METHOD, EXTEND_NATIVE_METHOD -> defineNativeMethod(nextString, currentFunction.constants[nextInt] as NativeFunction, code == EXTEND_NATIVE_METHOD)
                FIELD -> defineField(nextString)
                INNER_CLASS -> {
                    val kind = SClass.Kind.from(nextByte)
                    val name = nextString
                    val klass = SClass(name, kind)
                    val outerClass = currentNonFinalizedClass
                    // Inner classes have qualified names such as Range.Iterator, but we want to store the
                    // field with the last component name only
                    val innerName = name.lastNameComponent()
                    outerClass.fields[innerName] = klass
                    nonFinalizedClasses.push(fiber.sp)
                    push(klass)
                    applyAnnotations(outerClass, innerName)
                }
                CLASS_DECLR_DONE -> {
                    (peek() as SClass).finalizeDeclr()
                    nonFinalizedClasses.pop()
                }
                EXTEND -> nonFinalizedClasses.push(fiber.sp - 1) // class being extended is already at the top of the stack
                EXTEND_DONE -> {
                    nonFinalizedClasses.pop()
                    fiber.sp-- // remove the extended class from stack
                }
                INIT_ENUM -> initEnum()
                SUPER -> {
                    val superclass = nextString
                    val klass = (self as? Instance)?.klass ?: self as SClass
                    klass.superclasses[superclass]?.let {
                        push(it)
                    } ?: throw runtimeError("Class ${klass.name} doesn't inherit from $superclass!")
                }
                GET_SUPER -> getSuper()
                SUPER_INVOKE -> invokeSuper(nextString, nextInt)
                SELF_FN -> push(fiber.frame.closure.function)
                OBJECT -> objectLiteral(nextByte == 1.toByte(), nextInt)
                LIST -> listLiteral(nextByte == 1.toByte(), nextInt)
                START_COMPREHENSION -> push(ObjectComprehension())
                ADD_TO_COMPREHENSION -> {
                    val value = pop()
                    // offset is 2 as at 1 is the iterable value, and at offset 0 lies the iterator
                    (peek(2) as ObjectComprehension).values += value
                }
                ANNOTATE -> {
                    val annotation = pop()
                    if (annotation == Nil) {
                        throw runtimeError("Nil annotation!")
                    }
                    annotationBuffer += annotation
                }
                ANNOTATE_FIELD -> {
                    val fieldName = nextString
                    val klass = peek() as SClass
                    applyAnnotations(klass, fieldName)
                }
                GET_ANNOTATIONS -> {
                    val value = pop()
                    (value as? SClass)?.let {
                        push(it.mergedAnnotations.toSimiObject())
                    } ?: throw runtimeError("Getting annotations only works on a Class!", value)
                }
                GU -> gu(currentFunction.debugInfo?.compiler)
                IVIC -> push(ivic(pop()))
                YIELD -> yield(pop())
                NIL_CHECK -> {
                    if (peek() == Nil) {
                        fiber.stack[fiber.sp - 1] = Instance(declaredClasses[Constants.EXCEPTION_NIL_REFERENCE]!!, false)
                    }
                }
                SPREAD -> {
                    val value = pop()
                    (value as? ListInstance)?.let {
                        for (item in it.items) {
                            push(item)
                        }
                    } ?: throw runtimeError("Can only spread lists.", value)
                }
            }
        }
    }

    private fun await(isRoot: Boolean, argCount: Int) {
        val caller = if (isRoot) null else fiber
        fiber = peek(argCount) as Fiber
        fiber.caller = caller
        try {
            if (isRoot || fiber.state == Fiber.State.NEW) {
                fiber.state = Fiber.State.STARTED
                if (!isRoot) {
                    push(fiber.closure)
                }
                moveArgsFromCaller(argCount)
                callClosure(fiber.closure, argCount)
            } else {
                moveArgsFromCaller(argCount)
                run(0)
            }
        } catch (yield: Yield) { }
    }

    private fun moveArgsFromCaller(argCount: Int) {
        fiber.caller?.let {
            for (i in 0 until argCount) {
                // copy args to the top (+1 for closure) of the fiber stack
                fiber.stack[i + 1] = it.stack[it.sp - argCount + i]
            }
            it.sp -= argCount // remove args from caller stack
            // if this is the first invocation and the the fiber stack is empty, bump its sp
            if (fiber.sp < argCount + 1) {
                fiber.sp = argCount + 1
            }
        }
    }

    private fun yield(value: Any) {
        closeUpvalues(fiber.frame.sp, fiber.name)
        fiber = fiber.caller!!
        fiber.sp-- // pop the fiber from the caller stack
        push(value)
        throw Yield()
    }

    private fun runLocal() {
        run(fiber.fp - 1)
    }

    private fun pushConst() {
        push(currentFunction.constants[nextInt])
    }

    private fun setVar() {
        fiber.stack[fiber.frame.sp + nextInt] = pop()
    }

    private fun getVar(sp: Int) {
        push(fiber.stack[fiber.frame.sp + sp]!!)
    }

    private fun setProp() {
        val value = pop()
        val prop = pop()
        val obj = pop()
        if (obj is Instance && !obj.mutable && obj != self) { // obj != self because @a = 3 must still work from inside of object's methods
            throw runtimeError("Attempting to set on an immutable object!", obj)
        }
        if (obj is SClass && obj.kind == SClass.Kind.MODULE) {
            throw runtimeError("Can't set values of a module!")
        }
        if (obj is Instance && obj.klass.overriddenSet != null) {
            fiber.sp++ // Just to compensate for bindMethod's fiber.sp--
            bindMethod(obj, obj.klass, obj.klass.overriddenSet, Constants.SET)
            push(prop)
            push(value)
            call(peek(2), 2)
            fiber.sp-- // A setter is still a statement so we need to remove the call result from the fiber.stack
        } else {
            setPropRaw(obj, prop, value)
        }
    }

    private fun setPropRaw(obj: Any?, prop: Any, value: Any) {
        getFieldsOrThrow(obj)[stringify(prop)] = value
    }

    private fun getProp() {
        val nameValue = pop()
        val obj = boxIfNotInstance(0)
        fiber.sp--
        if (obj is Instance && obj.klass.overriddenGet != null) {
            fiber.sp++ // Just to compensate for bindMethod's fiber.sp--
            bindMethod(obj, obj.klass, obj.klass.overriddenGet, Constants.GET)
            push(nameValue)
            call(peek(1), 1)
        } else {
            getPropRaw(obj, stringify(nameValue))
        }
    }

    internal fun getPropRaw(obj: Any?, name: String) {
        val prop = when (obj) {
            Nil -> null
            is Instance -> obj.fields[name] ?: obj.klass.fields[name]
            is SClass -> obj.fields[name]
            else -> throw runtimeError("Only instances can have fields!", obj)
        }
        push(prop ?: Nil)
        if (prop != null) {
            bindMethod(obj, getSClass(obj), prop, name)
        }
    }

    /**
     * The algorithm is as follows:
     * 1. At the beginning, the top contains the right-hand side value and the prepared, unexecuted getter. Pop the value and keep references to object and prop for setting.
     * 2. getProp() to convert the prop and obj to a single value.
     * 3. Add the value again to the top, then invoke a single run with the next code, which the the op data of UPDATE_PROP. It'll pop the value and the prop and combine them into a single value.
     * 4. Pop this combined value and prepare for a setter - push the object, the prop and finally the combined value again.
     * 5. Invoke setProp() to store it where it belongs.
     */
    private fun updateProp() {
        var value = pop()
        val prop = peek()
        val obj = peek(1)
        getProp()
        push(value)
        val nextBufferPos = buffer.position() + 1
        run(fiber.fp) { buffer.position() < nextBufferPos }
        value = pop()
        push(obj)
        push(prop)
        push(value)
        setProp()
    }

    private fun getSuper() {
        val name = stringify(pop())
        val superclass = pop() as SClass
        val prop = superclass.fields[name]
        push(prop ?: Nil)
        (self as? Instance)?.let {
            bindMethod(it, superclass, prop, name)
        }
    }

    private fun isFalsey(o: Any): Boolean {
        return o == false
    }

    private fun isException(o: Any?): Boolean {
        return o is Instance && o.klass.checkIs(declaredClasses[Constants.CLASS_EXCEPTION]!!)
    }

    private fun invert() {
        val a = unbox(pop())
        push(isFalsey(a))
    }

    private fun negate() {
        val a = unbox(pop())
        if (a is Long) {
            push(-a)
        } else if (a is Double) {
            push(-a)
        }
    }

    private fun add() {
        val b = unbox(pop())
        val a = unbox(pop())
        push(when (a) {
            is String -> a + stringify(b)
            is ListInstance -> {
                if (a.mutable) {
                    a += b
                    a
                } else {
                    mutabilityLockException()
                }
            }
            else -> {
                if (b is String) {
                    stringify(a) + b
                } else {
                    binaryOp(ADD, a, b)
                }
            }
        })
    }

    private fun binaryOpOnStack(opCode: OpCode) {
        val b = pop()
        val a = pop()
        push(binaryOp(opCode, a, b))
    }

    private fun binaryOp(opCode: OpCode, a: Any, b: Any): Any {
        if (a == Nil || b == Nil) {
            return Nil
        }
        val realA = unbox(a)
        val realB = unbox(b)
        return if (realA is Long && realB is Long) {
            binaryOpTwoLongs(opCode, realA, realB)
        } else {
            val d1 = if (realA is Double) realA else (realA as Long).toDouble()
            val d2 = if (realB is Double) realB else (realB as Long).toDouble()
            binaryOpTwoDoubles(opCode, d1, d2)
        }
    }

    private fun binaryOpTwoLongs(opCode: OpCode, a: Long, b: Long): Any {
        return when (opCode) {
            ADD -> a + b
            SUBTRACT -> a - b
            MULTIPLY -> a * b
            DIVIDE -> intIfPossible(a * 1.0 / b)
            DIVIDE_INT -> a / b
            MOD -> a % b
            LT -> a < b
            LE -> a <= b
            GE -> a >= b
            GT -> a > b
            else -> throw runtimeError("WTF")
        }
    }

    private fun binaryOpTwoDoubles(opCode: OpCode, a: Double, b: Double): Any {
        return when (opCode) {
            ADD -> intIfPossible(a + b)
            SUBTRACT -> intIfPossible(a - b)
            MULTIPLY -> intIfPossible(a * b)
            DIVIDE -> intIfPossible(a / b)
            MOD ->intIfPossible(a % b)
            LT -> a < b
            LE -> a <= b
            GE -> a >= b
            GT -> a > b
            else -> throw runtimeError("WTF")
        }
    }

    private fun intIfPossible(d: Double): Any {
        val rounded = round(d)
        return if (rounded == d) {
            rounded.toLong()
        } else {
            d
        }
    }

    private fun checkEquality(code: OpCode) {
        val b = pop()
        val a = pop()
        val r = areEqual(a, b)
        push(if (code == EQ) r else !r)
    }

    private fun areEqual(a: Any, b: Any): Boolean {
        if (b == Nil) {
            return a == Nil
        }
        return when (a) {
            b -> true
            Nil -> b == Nil
            is Function -> {
                if (b is Closure) {
                    a == b.function
                } else {
                    false
                }
            }
            is Closure -> areEqual(a.function, b)
            is Instance -> {
                push(a)
                push(b)
                val foundMethod = invoke(Constants.EQUALS, 1, false)
                if (foundMethod) {
                    !isFalsey(pop())
                } else {
                    fiber.sp -= 2 // pop the args
                    false
                }
            }
            else -> false
        }
    }

    private fun checkIs() {
        val b = pop() as SClass
        val a = boxIfNotInstance(0)
        fiber.sp-- // Pop a
        val r = when (a) {
            Nil -> false
            is Instance -> a.klass.checkIs(b)
            is SClass -> if (b == declaredClasses[Constants.CLASS_CLASS]!!) true else a.checkIs(b)
            else -> throw RuntimeException("WTF")
        }
        push(r)
    }

    private fun jumpIf(predicate: (Any) -> Boolean) {
        val offset = nextInt
        if (offset != Compiler.CALL_DEFAULT_JUMP_LOCATION && predicate(peek())) {
            buffer.position(offset)
        }
    }

    private fun closure(asFiber: Boolean) {
        val function = currentFunction.constants[nextInt] as Function
        val closure = Closure(function)
        if (asFiber) {
            push(FiberTemplate(closure))
        } else {
            push(closure)
        }
        for (i in 0 until function.upvalueCount) {
            val isLocal = nextByte == 1.toByte()
            val sp = nextInt
            val fiberName = nextString
            closure.upvalues[i] = if (isLocal) {
                captureUpvalue(fiber.frame.sp + sp, fiberName)
            } else {
                fiber.frame.closure.upvalues[sp]
            }
        }
    }

    private fun closeUpvalue() {
        closeUpvalues(fiber.sp - 1, fiber.name)
        fiber.sp--
    }

    private fun call(callee: Any, argCount: Int) {
        val spRelativeToArgCount = fiber.sp - argCount - 1
        when (callee) {
            is Closure -> {
                fiber.stack[spRelativeToArgCount] = self
                callClosure(callee, argCount)
            }
            is BoundMethod -> {
                fiber.stack[spRelativeToArgCount] = callee.receiver
                callClosure(callee.method, argCount)
            }
            is BoundNativeMethod -> {
                fiber.stack[spRelativeToArgCount] = callee.receiver
                callNative(callee.method, argCount)
            }
            is NativeFunction -> callNative(callee, argCount)
            is SClass -> {
                if (callee.kind == SClass.Kind.META) { // Metaclasses (Num, String, Function, Class) can't be directly instantiated
                    throw runtimeError("Can't instantiate ${callee.name}!")
                }
                if (callee.kind == SClass.Kind.MODULE) {
                    throw runtimeError("Can't call a module!")
                }
                if (callee.kind == SClass.Kind.ENUM && callee != self) { // when callee == self, we're inside #initEnum method
                    throw runtimeError("Can't instantiate enums!")
                }
                fiber.stack[spRelativeToArgCount] = Instance(callee, callee.kind == SClass.Kind.OPEN)
                val init = callee.fields[Constants.INIT]
                when (init) {
                    is Closure -> callClosure(init, argCount)
                    is NativeFunction -> callNative(init, argCount)
                }
            }
            is FiberTemplate -> {
                if (argCount != 0) {
                    throw runtimeError("A fiber can only be instantiated with 0 arguments.")
                }
                fiber.stack[spRelativeToArgCount] = Fiber(callee.closure)
            }
            is Fiber -> {
                await(false, argCount)
            }
            is Proc -> {
                if (argCount != 0) {
                    throw runtimeError("A proc can only be called with 0 arguments.")
                }
                fiber.sp-- // pop the proc off the stack
                with(fiber.frame.buffer) {
                    val current = position() // store current position
                    position(callee.start) // jump to proc start
                    run(fiber.fp) { position() != callee.end } // run until code reaches end
                    position(current) // jump back to the call site
                }
            }
        }
    }

    private fun callClosure(closure: Closure, argCount: Int) {
        val f = closure.function
        handleOptionalParams(f, argCount)
        if (fiber.fp == MAX_FRAMES) {
            throw runtimeError("Stack overflow.")
        }
        fiber.callFrames[fiber.fp] = CallFrame(closure, fiber.sp - f.arity - 1)
        fiber.fp++
        debugger?.triggerBreakpoint(true)
        runLocal()
    }

    private fun handleOptionalParams(f: OptionalParamsFunc, argCount: Int) {
        if (argCount != f.arity) {
            if (argCount < f.arity
                    && f.optionalParamsStart != -1
                    && argCount >= f.optionalParamsStart) {
                for (i in argCount until (f.optionalParamsStart + f.defaultValues!!.size)) {
                    push(f.defaultValues!![i - f.optionalParamsStart])
                }
            } else {
                throw runtimeError("Expected ${f.arity} arguments but got $argCount.")
            }
        }
    }

    private fun callNative(method: NativeFunction, argCount: Int) {
        handleOptionalParams(method, argCount)
        val args = mutableListOf<Any?>()
        for (i in fiber.sp - method.arity - 1 until fiber.sp) {
            args += fiber.stack[i]
        }
        val result = method.func(args) ?: Nil
        fiber.sp -= argCount + 1
        push(result)
    }

    private fun invoke(name: String, argCount: Int, checkError: Boolean = true): Boolean {
        val receiver = boxIfNotInstance(argCount)
        if (receiver == Nil) {
            fiber.sp -= argCount // just remove the args, the receiver at slot fiber.sp - args - 1 is already Nil
            return true
        }
        return (receiver.fields[name])?.let {
            val callee = when (it) {
                is Closure -> BoundMethod(receiver, it)
                is NativeFunction -> BoundNativeMethod(receiver, it)
                else -> it
            }
            fiber.stack[fiber.sp - argCount - 1] = callee
            call(callee, argCount)
            true
        } ?: invokeFromClass(receiver, (receiver as? Instance)?.klass, name, argCount, checkError)
    }

    private fun invokeFromClass(receiver: Any?, klass: SClass?, name: String, argCount: Int, checkError: Boolean = true): Boolean {
        return when (val method = klass?.fields?.get(name)) {
            is Closure -> {
                callClosure(method, argCount)
                true
            }
            is NativeFunction -> {
                callNative(method, argCount)
                true
            }
            else -> {
                if (checkError) {
                    throw runtimeError("Undefined method $name.", receiver, klass)
                } else {
                    false
                }
            }
        }
    }

    private fun invokeSuper(name: String, argCount: Int) {
        val superclass = peek(argCount) as SClass
        if (superclass.name == Constants.CLASS_OBJECT) {
            if (name == Constants.GET && argCount == 1) {
                getPropRaw(self, pop() as String)
                val result = pop()
                fiber.sp-- // pop superclass
                push(result)
                return
            } else if (name == Constants.SET && argCount == 2) {
                val value = pop()
                setPropRaw(self, pop(), value)
                fiber.sp-- // pop superclass
                return
            }
        }
        set(argCount, self!!) // bind the super method to self
        invokeFromClass(superclass, superclass, name, argCount)
    }

    private fun objectLiteral(isMutable: Boolean, propCount: Int) {
        push(Instance(declaredClasses[Constants.CLASS_OBJECT]!!, isMutable).apply {
            if (propCount == -1) {
                val comprehension = pop() as ObjectComprehension
                var i = 0
                while (i < comprehension.values.size) {
                    fields[comprehension.values[i] as String] = comprehension.values[i + 1]
                    i += 2
                }
            } else {
                var i = propCount - 1
                while (i >= 0) {
                    val value = pop()
                    val key = pop() as String
                    fields[key] = value
                    i--
                }
            }
        })
    }

    private fun listLiteral(isMutable: Boolean, propCount: Int) {
        val items: List<Any>
        if (propCount == -1) {
            val comprehension = pop() as ObjectComprehension
            items = comprehension.values
        } else {
            items = mutableListOf()
            for (i in 0 until propCount) {
                val value = pop()
                items += value
            }
            items.reverse()
        }
        push(ListInstance(isMutable, items))
    }

    internal fun gu(enclosingCompiler: Compiler?) {
        enclosingCompiler?.let { enclosing ->
            (pop() as? String)?.let {
                try {
                    val tokens = Lexer("gu", "return $it\n", null).scanTokens(true).tokens
                    val compiler = Compiler(enclosing)
                    val f = compiler.compileLambdaForGu(tokens)
                    val closure = Closure(f)
                    push(closure)
                    for (i in 0 until f.upvalueCount) {
                        val upvalue = compiler.upvalues[i]
                        closure.upvalues[i] = if (upvalue.isLocal) {
                            captureUpvalue(fiber.frame.sp + upvalue.sp, upvalue.fiberName)
                        } else {
                            fiber.frame.closure.upvalues[upvalue.sp]
                        }
                    }
                    callClosure(closure, 0)
                } catch (e: Exception) {
                    push(Instance(declaredClasses[Constants.CLASS_EXCEPTION]!!, false).apply {
                        fields[Constants.MESSAGE] = e.message!!
                    })
                }
            } ?: throw runtimeError("'gu' must have a string argument.")
        } ?: throw runtimeError("'gu' can only be used in debug mode.")
    }

    internal fun ivic(value: Any): Any {
        return when (value) {
            is String, is Long, is Double, is Boolean -> value
            is Function -> value.ivic()
            is Closure -> value.function.ivic()
            is BoundMethod -> value.method.function.ivic()
            is Fiber -> value.closure.function.ivic()
            is FiberTemplate -> value.closure.function.ivic()
//            is NativeFunction -> CANT DO THIS
            is Instance -> stringify(value)
            else -> throw IllegalArgumentException("WTF")
        } ?: throw runtimeError("'ivic' can only be used in debug mode.")
    }

    private fun captureUpvalue(sp: Int, fiberName: String): Upvalue {
        var prevUpvalue: Upvalue? = null
        var upvalue = openUpvalues[fiberName]
        while (upvalue != null && upvalue.sp > sp) {
            prevUpvalue = upvalue
            upvalue = upvalue.next
        }
        if (upvalue != null && upvalue.sp == sp) {
            return upvalue
        }
        val createdUpvalue = Upvalue(WeakReference(sp), fiberName, false, upvalue)
        if (prevUpvalue == null) {
            openUpvalues[fiberName] = createdUpvalue
        } else {
            prevUpvalue.next = createdUpvalue
        }
        return createdUpvalue
    }

    private fun closeUpvalues(last: Int, fiberName: String) {
        var openUpvalue = openUpvalues[fiberName]
        while (openUpvalue != null && openUpvalue.sp >= last) {
            val upvalue = openUpvalue.apply {
                closed = true
                location = WeakReference(readUpvalueFromStack(this))
            }
            openUpvalue = upvalue.next
        }
        if (openUpvalue != null) {
            openUpvalues[fiberName] = openUpvalue
        } else {
            openUpvalues.remove(fiberName)
        }
    }

    private fun readUpvalue(upvalue: Upvalue): Any {
        return if (upvalue.closed) {
            upvalue.location.get()!!
        } else {
            readUpvalueFromStack(upvalue)
        }
    }

    private fun readUpvalueFromStack(upvalue: Upvalue): Any {
        var currentFiber: Fiber? = fiber
        while (currentFiber != null && currentFiber.name != upvalue.fiberName) {
            currentFiber = currentFiber.caller
        }
        currentFiber?.let {
            if (upvalue.sp < it.sp) {
                val value = it.stack[upvalue.sp]
                if (value != null) {
                    return value
                }
            }
        }
        throw runtimeError("Unable to read upvalue from stack.")
    }

    /**
     * @return true if the program should terminate
     */
    private fun doReturn(breakAtFp: Int): Boolean {
        val result = pop()
        val returningFrame = fiber.frame
        closeUpvalues(returningFrame.sp, fiber.name)
        val numberOfPops = nextInt
        for (i in 0 until numberOfPops) {
            val code = nextCode
            when (code) { // TODO move somewhere else, reuse existing code
                POP -> fiber.sp--
                CLOSE_UPVALUE -> closeUpvalue()
                POP_UNDER -> fiber.sp -= nextInt + 1 // + 1 is to pop the value on the fiber.stack as well
                else -> throw IllegalArgumentException("Unexpected code in return scope closing patch: $code!")
            }
        }
        fiber.fp--
        return if (fiber.fp == 0) { // Returning from top-level func
            fiber.sp = 0
            if (fiber.caller != null) {
                fiber.state = Fiber.State.NEW
                yield(result)
            }
            true
        } else {
            fiber.sp = returningFrame.sp
            push(result)
            fiber.fp == breakAtFp
        }
    }

    private fun declareClass(kind: SClass.Kind, name: String) {
        val klass = SClass(name, kind)
        declaredClasses[name] = klass
        nonFinalizedClasses.push(fiber.sp)
        push(klass)
        applyAnnotations(klass, klass.name)
    }

    private fun inherit() {
        val superclass = pop()
        if (superclass !is SClass) {
            throw runtimeError("Superclass must be a class.")
        }
        if (superclass.kind == SClass.Kind.FINAL) {
            throw runtimeError("Can't inherit from a final superclass ${superclass.name}")
        }

        if (superclass.kind == SClass.Kind.MODULE) {
            throw runtimeError("Can't inherit from a module ${superclass.name}")
        }
        val subclass = peek()
        (subclass as SClass).let {
            it.fields.putAll(superclass.fields)
            it.superclasses[superclass.name] = superclass
        }
    }

    private fun mixin(isExtension: Boolean) {
        val mixin = pop()
        if (mixin !is SClass) {
            throw runtimeError("Mixin must be a class.")
        }
        val klass = peek()
        (klass as SClass).let {
            if (it.kind == SClass.Kind.MODULE) {
                throw runtimeError("Can't mixin a module!")
            }
            for (field in mixin.fields) {
                if (!field.key.startsWith(Constants.PRIVATE)) { // Add public fields only
                    if (isExtension && field.key in it.fields.keys) {
                        continue
                    }
                    it.fields.putIfAbsent(field.key, field.value)
                }
            }
        }
    }

    private fun defineMethod(name: String, isExtension: Boolean) {
        val method = pop()
        val klass = currentNonFinalizedClass
        if (isExtension && name in klass.fields.keys) {
            throw runtimeError("Extension method issue: $name is already present in ${klass.name},")
        }
        klass.fields[name] = method
        applyAnnotations(klass, name)
    }

    private fun defineNativeMethod(name: String, method: NativeFunction, isExtension: Boolean) {
        val klass = currentNonFinalizedClass
        if (isExtension && name in klass.fields.keys) {
            throw runtimeError("Extension method issue: $name is already present in ${klass.name},")
        }
        klass.fields[name] = method
        applyAnnotations(klass, name)
    }

    private fun defineField(name: String) {
        val value = pop()
        val klass = currentNonFinalizedClass
        klass.fields[name] = value
        applyAnnotations(klass, name)
    }

    /**
     * 1. The enum class was just finalized and it's at the top of the stack.
     * 2. Get its #initEnum method.
     * 3. Prepare the call by pushing the class again so that it is "self" in the function's body.
     * 4. Invoke the function, clean up the Nil, and remove the method from the fields list.
     */
    private fun initEnum() {
        val klass = peek() as SClass
        val initEnumMethod = klass.fields[Constants.ENUM_INIT] as Closure
        push(klass) // as receiver
        callClosure(initEnumMethod, 0)
        fiber.sp-- // pop the NIL left by the function return
        klass.fields.remove(Constants.ENUM_INIT)
    }

    private fun resizeStackIfNecessary() {
        if (fiber.sp == fiber.stackSize) {
            gc()
            fiber.stackSize *= STACK_GROWTH_FACTOR
            fiber.stack = fiber.stack.copyOf(fiber.stackSize)
        }
    }

    internal fun push(o: Any) {
        resizeStackIfNecessary()
        fiber.stack[fiber.sp] = o
        fiber.sp++
    }

    internal fun pop(): Any {
        fiber.sp--
        return fiber.stack[fiber.sp]!!
    }

    private fun peek(offset: Int = 0): Any {
        return fiber.stack[fiber.sp - offset - 1]!!
    }

    private fun set(offset: Int, value: Any) {
        fiber.stack[fiber.sp - offset - 1] = value
    }

    private fun getFieldsOrThrow(it: Any?): MutableMap<String, Any> {
        return when (it) {
            is Instance -> it.fields
            is SClass -> it.fields
            else -> throw runtimeError("Only instances can have fields!", it)
        }
    }

    private fun getSClass(it: Any): SClass {
        return when (it) {
            is Instance -> it.klass
            is SClass -> it
            else -> throw runtimeError("Unable to get SClass for $it!", it)
        }
    }

    private fun bindMethod(receiver: Any, klass: SClass, prop: Any?, name: String) {
        getBoundMethod(receiver, klass, prop, name)?.let {
            fiber.sp--
            push(it)
        }
    }

    private fun getBoundMethod(receiver: Any, klass: SClass?, prop: Any?, name: String): Any? {
        return if (prop is NativeFunction) {
            BoundNativeMethod(receiver, prop)
        } else {
            val method = if (prop is Closure) {
                prop
            } else {
                (klass?.fields?.get(name) as? Closure)?.let { it } ?: return null
            }
            BoundMethod(receiver, method)
        }
    }

    /**
     * Check if peek(offset) is instance, if it isn't it tries to box it.
     */
    private fun boxIfNotInstance(offset: Int): Fielded {
        val loc = fiber.sp - offset - 1
        val value = fiber.stack[loc]
        if (value == Nil) {
            return Nil
        } else if (value is Instance || value is SClass) {
            return value as Fielded
        } else if (value is Long || value is Double) {
            val boxedNum = Instance(declaredClasses[Constants.CLASS_NUM]!!, false).apply {
                fields[Constants.PRIVATE] = value
            }
            fiber.stack[loc] = boxedNum
            return boxedNum
        } else if (value is String) {
            val boxedStr = Instance(declaredClasses[Constants.CLASS_STRING]!!, false).apply {
                fields[Constants.PRIVATE] = value
                fields["len"] = value.length.toLong()
            }
            fiber.stack[loc] = boxedStr
            return boxedStr
        } else if (value is Closure) {
            val boxedClosure = Instance(declaredClasses[Constants.CLASS_FUNCTION]!!, false).apply {
                fields[Constants.PRIVATE] = value
                fields[Constants.NAME] = value.function.name
                fields[Constants.ARITY] = value.function.arity
            }
            fiber.stack[loc] = boxedClosure
            return boxedClosure
        } else {
            throw runtimeError("Unable to box $value!", value)
        }
    }

    private fun unbox(o: Any): Any {
        return if (o is Instance) {
            when (o.klass) {
                declaredClasses[Constants.CLASS_NUM], declaredClasses[Constants.CLASS_STRING] -> o.fields[Constants.PRIVATE]!!
                else -> o
            }
        } else {
            o
        }
    }

    internal fun stringify(o: Any?): String {
        return when (o) {
            is Instance -> {
                when (o.klass) {
                    declaredClasses[Constants.CLASS_OBJECT] -> o.stringify(this)
                    declaredClasses[Constants.CLASS_LIST] -> o.stringify(this)
                    declaredClasses[Constants.CLASS_STRING] -> o.fields[Constants.PRIVATE] as String
                    else -> {
                        when (val toStringField = o.fields[Constants.TO_STRING] ?: o.klass.fields[Constants.TO_STRING]) {
                            is Closure, is NativeFunction -> {
                                push(o) // need to push as bound method will replace this index with itself
                                call(getBoundMethod(o, o.klass, toStringField, Constants.TO_STRING)!!, 0)
                                pop() as String
                            }
                            else -> o.stringify(this)
                        }
                    }
                }
            }
            else -> o.toString()
        }
    }

    private fun applyAnnotations(klass: SClass, name: String) {
        if (annotationBuffer.isNotEmpty()) {
            klass.annotations[name] = annotationBuffer.toTypedArray()
            annotationBuffer.clear()
        }
    }

    private fun gc() {
        for (i in fiber.sp until fiber.stackSize) {
            fiber.stack[i] = null
        }
        System.gc()
    }

    private fun printStack() {
        println(fiber.stack.copyOfRange(0, fiber.sp).joinToString(" "))
    }

    private fun runtimeError(message: String, vararg operands: Any?): Exception {
        println()
        println(message)
        for (operand in operands) {
            if (isException(operand)) {
                println("Found an unhandled exception: ${stringify(operand)}. Are you missing a 'catch' block?")
            }
        }
        printCallStack()
        return IllegalStateException()
    }

    private fun printCallStack() {
        for (i in fiber.fp - 1 downTo 0) {
            println(fiber.callFrames[i])
        }
    }

    internal val buffer: ByteBuffer get() = fiber.frame.buffer
    internal val currentFunction: Function get() = fiber.frame.closure.function
    private val currentNonFinalizedClass: SClass get() = fiber.stack[nonFinalizedClasses.peek()] as SClass
    private val self: Any? get() = fiber.stack[fiber.frame.sp] // self is always at 0

    private val nextCode: OpCode get() = OpCode.from(nextByte)
    private val nextByte: Byte get() = buffer.get()
    private val nextInt: Int get() = buffer.int
    private val nextLong: Long get() = buffer.long
    private val nextDouble: Double get() = buffer.double
    private val nextString: String get() = currentFunction.constants[nextInt] as String

    private class Yield : RuntimeException("Yield")

    private class ObjectComprehension {
        val values = mutableListOf<Any>()
    }

    companion object {
        internal const val INITIAL_STACK_SIZE = 256
        private const val STACK_GROWTH_FACTOR = 4
        internal const val MAX_FRAMES = 1024

        internal var instance: Vm? = null
        val declaredClasses = mutableMapOf<String, SClass>()

        fun newInstance(className: String, init: Instance.() -> Unit): Instance {
            val instance = Instance(declaredClasses[className]!!, false)
            instance.init()
            return instance
        }

        fun newObject(init: Instance.() -> Unit) = newInstance(Constants.CLASS_OBJECT, init)
    }
}