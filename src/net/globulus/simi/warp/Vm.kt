package net.globulus.simi.warp

import net.globulus.simi.Constants
import net.globulus.simi.warp.OpCode.*
import net.globulus.simi.warp.native.NativeFunction
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import kotlin.math.round

class Vm {
    private lateinit var fiber: Fiber
    private var openUpvalues: Upvalue? = null
    private val annotationBuffer = mutableListOf<Any>()

    fun interpret(input: Fiber) {
        fiber = input
        push(input)
        try {
            await(true, 0)
        } catch (ignored: IllegalStateException) { } // Silently abort the program
    }

    private fun run(breakAtFp: Int, once: Boolean = false) {
        loop@ while (true) {
            val code = nextCode
            when (code) {
                CONST_INT -> push(nextLong)
                CONST_FLOAT -> push(nextDouble)
                CONST_ID -> push(nextString)
                CONST -> pushConst(fiber.frame)
                NIL -> push(Nil)
                POP -> fiber.sp--
                POP_UNDER -> {
                    val value = pop()
                    fiber.sp -= nextInt
                    push(value)
                }
                DUPLICATE -> push(peek())
                SET_LOCAL -> setVar(fiber.frame)
                GET_LOCAL -> getVar(fiber.frame)
                SET_UPVALUE -> fiber.frame.closure.upvalues[nextInt]!!.location = WeakReference(fiber.sp - 1) // -1 because we need to point at an actual slot
                GET_UPVALUE -> push(readUpvalue(fiber.frame.closure.upvalues[nextInt]!!))
                SET_PROP -> setProp()
                GET_PROP -> getProp()
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
                PRINT -> println(pop())
                JUMP -> buffer.position(nextInt)
                JUMP_IF_FALSE -> jumpIf { isFalsey(it) }
                JUMP_IF_NIL -> jumpIf { it == Nil }
                JUMP_IF_EXCEPTION -> jumpIf { it is Instance && it.klass.checkIs(exceptionClass!!) }
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
                CLOSE_UPVALUE -> closeUpvalue()
                RETURN -> {
                    if (doReturn(nextString, breakAtFp)) {
                        break@loop
                    }
                }
                CLASS -> declareClass(SClass.Kind.from(nextByte), nextString)
                INHERIT -> inherit()
                IMPORT -> mixin()
                METHOD -> defineMethod(nextString)
                NATIVE_METHOD -> defineNativeMethod(nextString, currentFunction.constants[nextInt] as NativeFunction)
                INNER_CLASS -> {
                    val kind = SClass.Kind.from(nextByte)
                    val name = nextString
                    val klass = SClass(name, kind)
                    val outerClass = peek() as SClass
                    // Inner classes have qualified names such as Range.Iterator, but we want to store the
                    // field with the last component name only
                    val innerName = name.lastNameComponent()
                    outerClass.fields[innerName] = klass
                    push(klass)
                    applyAnnotations(outerClass, innerName)
                }
                CLASS_DECLR_DONE -> (peek() as SClass).finalizeDeclr()
                SUPER -> {
                    val superclass = nextString
                    val klass = (self as? Instance)?.klass ?: self as SClass
                    klass.superclasses[superclass]?.let {
                        push(it)
                    } ?: throw runtimeError("Class ${klass.name} doesn't inherit from $superclass!")
                }
                GET_SUPER -> getSuper()
                SUPER_INVOKE -> invokeSuper(nextString, nextInt)
                SELF_DEF -> push(fiber.frame.closure.function)
                OBJECT -> objectLiteral(nextByte == 1.toByte(), nextInt)
                LIST -> listLiteral(nextByte == 1.toByte(), nextInt)
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
                    (pop() as? SClass)?.let {
                        push(it.annotations.toSimiObject())
                    } ?: throw runtimeError("Getting annotations only works on a Class!")
                }
                GU -> gu()
                AWAIT -> await(false, nextInt)
                YIELD -> yield(pop())
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
                fiber.stack[i + 1] = it.stack[it.sp - argCount + i]
            }
            it.sp -= argCount
            if (fiber.sp < argCount + 1) {
                fiber.sp = argCount + 1
            }
        }
    }

    private fun yield(value: Any) {
        closeUpvalues(fiber.frame.sp)
        fiber = fiber.caller!!
        fiber.sp-- // pop the fiber from the caller stack
        push(value)
        throw Yield()
    }

    private fun runLocal() {
        run(fiber.fp - 1)
    }

    private fun pushConst(frame: CallFrame) {
        push(currentFunction.constants[nextInt])
    }

    private fun setVar(frame: CallFrame) {
        fiber.stack[fiber.frame.sp + nextInt] = pop()
    }

    private fun getVar(frame: CallFrame) {
        push(fiber.stack[fiber.frame.sp + nextInt]!!)
    }

    private fun setProp() {
        val value = pop()
        val prop = pop()
        val obj = pop()
        if (obj is Instance && !obj.mutable && obj != self) { // obj != self because @a = 3 must still work from inside of object's methods
            throw runtimeError("Attempting to set on an immutable object!")
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

    private fun getPropRaw(obj: Any?, name: String) {
        val prop = when (obj) {
            Nil -> null
            is Instance -> obj.fields[name] ?: obj.klass.fields[name]
            is SClass -> obj.fields[name]
            else -> throw runtimeError("Only instances can have fields!")
        }
        push(prop ?: Nil)
        if (prop != null) {
            bindMethod(obj, getSClass(obj), prop, name)
        }
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

    private fun getOuterFrame(): CallFrame {
        return fiber.callFrames[nextInt]!!
    }

    private fun isFalsey(o: Any): Boolean {
        return when (unbox(o)) {
            Nil -> true
            is Long -> o == 0L
            is Double -> o == 0.0
            else -> false
        }
    }

    private fun invert() {
        val a = unbox(pop())
        push(if (isFalsey(a)) 1L else 0L)
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
            LT -> boolToLong(a < b)
            LE -> boolToLong(a <= b)
            GE -> boolToLong(a >= b)
            GT -> boolToLong(a > b)
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
            LT -> boolToLong(a < b)
            LE -> boolToLong(a <= b)
            GE -> boolToLong(a >= b)
            GT -> boolToLong(a > b)
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

    private fun boolToLong(b: Boolean) = if (b) 1L else 0L

    private fun checkEquality(code: OpCode) {
        val b = pop()
        val a = pop()
        val r = areEqual(a, b)
        push(boolToLong(if (code == EQ) r else !r))
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
            is SClass -> if (b == classClass) true else a.checkIs(b)
            else -> throw RuntimeException("WTF")
        }
        push(boolToLong(r))
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
        closeUpvalues(fiber.sp - 1)
        fiber.sp--
    }

    private fun call(callee: Any, argCount: Int) {
        val spRelativeToArgCount = fiber.sp - argCount - 1
        when (callee) {
            is Closure -> callClosure(callee, argCount)
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
            val callee = if (it is NativeFunction) {
                BoundNativeMethod(receiver, it)
            } else {
                it
            }
            fiber.stack[fiber.sp - argCount - 1] = callee
            call(callee, argCount)
            true
        } ?: invokeFromClass((receiver as? Instance)?.klass, name, argCount, checkError)
    }

    private fun invokeFromClass(klass: SClass?, name: String, argCount: Int, checkError: Boolean = true): Boolean {
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
                    throw runtimeError("Undefined method $name.")
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
        invokeFromClass(superclass, name, argCount)
    }

    private fun objectLiteral(isMutable: Boolean, propCount: Int) {
        push(Instance(objectClass!!, isMutable).apply {
            var i = propCount - 1
            while (i >= 0) {
                val value = pop()
                val key = pop() as String
                fields[key] = value
                i--
            }
        })
    }

    private fun listLiteral(isMutable: Boolean, propCount: Int) {
        val items = mutableListOf<Any>()
        for (i in 0 until propCount) {
            items += pop()
        }
        items.reverse()
        push(ListInstance(isMutable, items))
    }

    private fun gu() {
        (pop() as? String)?.let {
            try {
            val tokens = Lexer("gu", "return $it\n", null).scanTokens(true).tokens
            val func = Compiler().compile(tokens)
            val closure = Closure(func)
            push(closure)
            callClosure(closure, 0)
            } catch (e: Exception) {
                push(Instance(exceptionClass!!, false).apply {
                    fields[Constants.MESSAGE] = e.message!!
                })
            }
        } ?: throw runtimeError("'gu' must have a string argument.")
    }

    private fun captureUpvalue(sp: Int, fiberName: String): Upvalue {
        var prevUpvalue: Upvalue? = null
        var upvalue = openUpvalues
        while (upvalue != null && upvalue.sp > sp) {
            prevUpvalue = upvalue
            upvalue = upvalue.next
        }
        if (upvalue != null && upvalue.sp == sp && upvalue.fiberName == fiberName) {
            return upvalue
        }
        val createdUpvalue = Upvalue(WeakReference(sp), fiberName, false, upvalue)
        if (prevUpvalue == null) {
            openUpvalues = createdUpvalue
        } else {
            prevUpvalue.next = createdUpvalue
        }
        return createdUpvalue
    }

    private fun closeUpvalues(last: Int) {
        while (openUpvalues != null && openUpvalues!!.sp >= last) {
            val upvalue = openUpvalues?.apply {
                closed = true
                location = WeakReference(readUpvalueFromStack(this))
            }
            openUpvalues = upvalue?.next
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
    private fun doReturn(from: String, breakAtFp: Int): Boolean {
        val result = pop()
        val frameToReturnFrom = if (from.isEmpty()) fiber.frame.name else from
        var returningFrame: CallFrame
        do {
            returningFrame = fiber.frame
            closeUpvalues(returningFrame.sp)
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
        } while (returningFrame.name != frameToReturnFrom)
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
        if (numClass == null && name == Constants.CLASS_NUM) {
            numClass = klass
        } else if (strClass == null && name == Constants.CLASS_STRING) {
            strClass = klass
        } else if (classClass == null && name == Constants.CLASS_CLASS) {
            classClass = klass
        } else if (funcClass == null && name == Constants.CLASS_FUNCTION) {
            funcClass = klass
        } else if (exceptionClass == null && name == Constants.CLASS_EXCEPTION) {
            exceptionClass = klass
        } else if (objectClass == null && name == Constants.CLASS_OBJECT) {
            objectClass = klass
        } else if (listClass == null && name == Constants.CLASS_LIST) {
            listClass = klass
        }
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
        val subclass = peek()
        (subclass as SClass).let {
            it.fields.putAll(superclass.fields)
            it.superclasses[superclass.name] = superclass
        }
    }

    private fun mixin() {
        val mixin = pop()
        if (mixin !is SClass) {
            throw runtimeError("Mixin must be a class.")
        }
        val klass = peek()
        (klass as SClass).let {
            for (field in mixin.fields) {
                if (!field.key.startsWith(Constants.PRIVATE)) { // Add public fields only
                    it.fields[field.key] = field.value
                }
            }
        }
    }

    private fun defineMethod(name: String) {
        val method = pop() as Closure
        val klass = peek() as SClass
        klass.fields[name] = method
        applyAnnotations(klass, name)
    }

    private fun defineNativeMethod(name: String, method: NativeFunction) {
        val klass = peek() as SClass
        klass.fields[name] = method
        applyAnnotations(klass, name)
    }

    private fun resizeStackIfNecessary() {
        if (fiber.sp == fiber.stackSize) {
            gc()
            fiber.stackSize *= STACK_GROWTH_FACTOR
            fiber.stack = fiber.stack.copyOf(fiber.stackSize)
        }
    }

    private fun push(o: Any) {
        resizeStackIfNecessary()
        fiber.stack[fiber.sp] = o
        fiber.sp++
    }

    private fun pop(): Any {
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
            else -> throw runtimeError("Only instances can have fields!")
        }
    }

    private fun getSClass(it: Any): SClass {
        return when (it) {
            is Instance -> it.klass
            is SClass -> it
            else -> throw runtimeError("Unable to get SClass for $it!")
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
            val boxedNum = Instance(numClass!!, false).apply {
                fields[Constants.PRIVATE] = value
            }
            fiber.stack[loc] = boxedNum
            return boxedNum
        } else if (value is String) {
            val boxedStr = Instance(strClass!!, false).apply {
                fields[Constants.PRIVATE] = value
            }
            fiber.stack[loc] = boxedStr
            return boxedStr
        } else if (value is Closure) {
            val boxedClosure = Instance(funcClass!!, false).apply {
                fields[Constants.PRIVATE] = value
                fields[Constants.NAME] = value.function.name
                fields[Constants.ARITY] = value.function.arity
            }
            fiber.stack[loc] = boxedClosure
            return boxedClosure
        } else {
            throw runtimeError("Unable to box $value!")
        }
    }

    private fun unbox(o: Any): Any {
        return if (o is Instance) {
            when (o.klass) {
                numClass, strClass -> o.fields[Constants.PRIVATE]!!
                else -> o
            }
        } else {
            o
        }
    }

    internal fun stringify(o: Any): String {
        return when (o) {
            is Instance -> {
                when (o.klass) {
                    objectClass -> o.stringify(this)
                    listClass -> o.stringify(this)
                    else -> {
                        when (val toStringField = o.fields[Constants.TO_STRING] ?: o.klass.fields[Constants.TO_STRING]) {
                            is Closure, is NativeFunction -> {
                                call(getBoundMethod(o, o.klass, toStringField, Constants.TO_STRING)!!, 0)
                                pop() as String
                            }
                            else -> o.toString()
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

    private fun runtimeError(message: String): Exception {
        println()
        println(message)
        printCallStack()
        return IllegalStateException()
    }

    private fun printCallStack() {
        for (i in fiber.fp - 1 downTo 0) {
            println(fiber.callFrames[i])
        }
    }

    private val buffer: ByteBuffer get() = fiber.frame.buffer
    private val currentFunction: Function get() = fiber.frame.closure.function
    private val self: Any? get() = fiber.stack[fiber.frame.sp] // self is always at 0

    private val nextCode: OpCode get() = OpCode.from(nextByte)
    private val nextByte: Byte get() = buffer.get()
    private val nextInt: Int get() = buffer.int
    private val nextLong: Long get() = buffer.long
    private val nextDouble: Double get() = buffer.double
    private val nextString: String get() = currentFunction.constants[nextInt] as String

    private class Yield : RuntimeException("Yield")

    companion object {
        internal const val INITIAL_STACK_SIZE = 256
        private const val STACK_GROWTH_FACTOR = 4
        internal const val MAX_FRAMES = 64

        var numClass: SClass? = null
            internal set
        var strClass: SClass? = null
            internal set
        var classClass: SClass? = null
            internal set
        var funcClass: SClass? = null
            internal set
        var exceptionClass: SClass? = null
            internal set
        var objectClass: SClass? = null
            internal set
        var listClass: SClass? = null
            internal set
    }
}