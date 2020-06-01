package net.globulus.simi.warp

import net.globulus.simi.Constants
import net.globulus.simi.warp.OpCode.*
import net.globulus.simi.warp.native.NativeFunction
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import kotlin.math.round

internal class Vm {
    private var sp = 0
    private var stackSize = INITIAL_STACK_SIZE
    private var stack = arrayOfNulls<Any>(INITIAL_STACK_SIZE)

    private var numClass: SClass? = null
    private var strClass: SClass? = null
    private var classClass: SClass? = null
    private var funcClass: SClass? = null
    private var exceptionClass: SClass? = null

    private var fp = 0
        set(value) {
            field = value
            if (value > 0) {
                frame = callFrames[value - 1]!!
            }
        }
    private lateinit var frame: CallFrame
    private val callFrames = arrayOfNulls<CallFrame>(MAX_FRAMES)

    private var openUpvalues: Upvalue? = null

    fun interpret(input: Function) {
        val closure = Closure(input)
        push(closure)
        callClosure(closure, 0)
        try {
            run(0)
            printStack()
        } catch (ignored: IllegalStateException) { } // Silently abort the program
    }

    private fun run(breakAtFp: Int, once: Boolean = false) {
        loop@ while (true) {
            val code = nextCode
            when (code) {
                CONST_INT -> push(nextLong)
                CONST_FLOAT -> push(nextDouble)
                CONST_ID -> push(nextString)
                CONST -> pushConst(frame)
//                CONST_OUTER -> pushConst(getOuterFrame())
                NIL -> push(Nil)
                POP -> sp--
                POP_UNDER -> {
                    val value = pop()
                    sp -= nextInt
                    push(value)
                }
                DUPLICATE -> push(peek())
                SET_LOCAL -> setVar(frame)
                GET_LOCAL -> getVar(frame)
                SET_UPVALUE -> frame.closure.upvalues[nextInt]!!.location = WeakReference(sp - 1) // -1 because we need to point at an actual slot
                GET_UPVALUE -> push(readUpvalue(frame.closure.upvalues[nextInt]!!))
                SET_PROP -> setProp()
                GET_PROP -> getProp()
                INVERT -> invert()
                NEGATE -> negate()
                ADD -> add()
                SUBTRACT, MULTIPLY, DIVIDE, DIVIDE_INT, MOD, LE, LT, GE, GT -> binaryOpOnStack(code)
                EQ -> checkEquality(code)
                IS -> checkIs()
                HAS -> {
                    val b = stack[sp - 1]
                    stack[sp - 1] = stack[sp - 2]
                    stack[sp - 2] = b
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
                CLOSURE -> {
                    val function = currentFunction.constants[nextInt] as Function
                    val closure = Closure(function)
                    push(closure)
                    for (i in 0 until function.upvalueCount) {
                        val isLocal = nextByte == 1.toByte()
                        val sp = nextInt
                        closure.upvalues[i] = if (isLocal) {
                            captureUpvalue(frame.sp + sp)
                        } else {
                            frame.closure.upvalues[sp]
                        }
                    }
                }
                CLOSE_UPVALUE -> closeUpvalue()
                RETURN -> {
                    if (doReturn(nextString, breakAtFp)) {
                        break@loop
                    }
                }
                CLASS -> {
                    val name = nextString
                    val klass = SClass(name)
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
                    }
                    push(klass)
                }
                INHERIT -> inherit()
                IMPORT -> mixin()
                METHOD -> defineMethod(nextString)
                NATIVE_METHOD -> defineNativeMethod(nextString, currentFunction.constants[nextInt] as NativeFunction)
                INNER_CLASS -> {
                    val name = nextString
                    val klass = SClass(name)
                    val outerClass = peek() as SClass
                    // Inner classes have qualified names such as Range.Iterator, but we want to store the
                    // field with the last component name only
                    outerClass.fields[name.lastNameComponent()] = klass
                    push(klass)
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
                SELF_DEF -> push(frame.closure.function)
            }
        }
    }

    private fun runLocal() {
        run(fp - 1)
    }

    private fun pushConst(frame: CallFrame) {
        push(currentFunction.constants[nextInt])
    }

    private fun setVar(frame: CallFrame) {
        stack[frame.sp + nextInt] = pop()
    }

    private fun getVar(frame: CallFrame) {
        push(stack[frame.sp + nextInt]!!)
    }

    private fun setProp() {
        val value = pop()
        val prop = pop()
        val obj = pop()
        if (obj is Instance && obj.klass.overridenSet != null) {
            sp++ // Just to compensate for bindMethod's sp--
            bindMethod(obj, obj.klass, obj.klass.overridenSet, Constants.SET)
            push(prop)
            push(value)
            call(peek(2), 2)
            runLocal()
            sp-- // A setter is still a statement so we need to remove the call result from the stack
        } else {
            setPropRaw(obj, prop, value)
        }
    }

    private fun setPropRaw(obj: Any?, prop: Any, value: Any) {
        getFieldsOrThrow(obj)[prop.toString()] = value
    }

    private fun getProp() {
        val nameValue = pop()
        val obj = boxIfNotInstance(0)
        sp--
        if (obj is Instance && obj.klass.overridenGet != null) {
            sp++ // Just to compensate for bindMethod's sp--
            bindMethod(obj, obj.klass, obj.klass.overridenGet, Constants.GET)
            push(nameValue)
            call(peek(1), 1)
            runLocal()
        } else {
            getPropRaw(obj, nameValue.toString())
        }
    }

    private fun getPropRaw(obj: Any?, name: String) {
        val prop = when (obj) {
            is Instance -> obj.fields[name] ?: obj.klass.fields[name]
            is SClass -> obj.fields[name]
            else -> throw runtimeError("Only instances can have fields!")
        }
        push(prop ?: Nil)
        bindMethod(obj, getSClass(obj), prop, name)
    }

    private fun getSuper() {
        val name = pop().toString()
        val superclass = pop() as SClass
        val prop = superclass.fields[name]
        push(prop ?: Nil)
        (self as? Instance)?.let {
            bindMethod(it, superclass, prop, name)
        }
    }

    private fun getOuterFrame(): CallFrame {
        return callFrames[nextInt]!!
    }

    private fun isFalsey(o: Any): Boolean {
        return when (o) {
            Nil -> true
            is Long -> o == 0L
            is Double -> o == 0.0
            else -> false
        }
    }

    private fun invert() {
        val a = pop()
        push(if (isFalsey(a)) 1L else 0L)
    }

    private fun negate() {
        val a = pop()
        if (a is Long) {
            push(-a)
        } else if (a is Double) {
            push(-a)
        }
    }

    private fun add() {
        val b = pop()
        val a = pop()
        push(when (a) {
            is String -> a + b
            else -> {
                if (b is String) {
                    a.toString() + b
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
        return if (a is Long && b is Long) {
            binaryOpTwoLongs(opCode, a, b)
        } else {
            val d1 = if (a is Double) a else (a as Long).toDouble()
            val d2 = if (b is Double) b else (b as Long).toDouble()
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
                    runLocal()
                    !isFalsey(pop())
                } else {
                    sp -= 2 // pop the args
                    false
                }
            }
            else -> false
        }
    }

    private fun checkIs() {
        val b = pop() as SClass
        val a = boxIfNotInstance(0)
        sp-- // Pop a
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
        if (predicate(peek())) {
            buffer.position(offset)
        }
    }

    private fun closeUpvalue() {
        closeUpvalues(sp - 1)
        sp--
    }

    private fun call(callee: Any, argCount: Int) {
        when (callee) {
            is Closure -> callClosure(callee, argCount)
            is BoundMethod -> {
                stack[sp - argCount - 1] = callee.receiver
                callClosure(callee.method, argCount)
            }
            is BoundNativeMethod -> {
                stack[sp - argCount - 1] = callee.receiver
                callNative(callee.method, argCount)
            }
            is NativeFunction -> callNative(callee, argCount)
            is SClass -> {
                stack[sp - argCount - 1] = Instance(callee)
                val init = callee.fields[Constants.INIT]
                when (init) {
                    is Closure -> callClosure(init, argCount)
                    is NativeFunction -> callNative(init, argCount)
                }
            }
        }
    }

    private fun callClosure(closure: Closure, argCount: Int) {
        val f = closure.function
        handleOptionalParams(f, argCount)
        if (fp == MAX_FRAMES) {
            throw runtimeError("Stack overflow.")
        }
        callFrames[fp] = CallFrame(closure, sp - f.arity - 1)
        fp++
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
        for (i in sp - method.arity - 1 until sp) {
            args += stack[i]
        }
        val result = method.func(args) ?: Nil
        sp -= argCount + 1
        push(result)
    }

    private fun invoke(name: String, argCount: Int, checkError: Boolean = true): Boolean {
        val receiver = boxIfNotInstance(argCount)
        return (receiver.fields[name])?.let {
            val callee = if (it is NativeFunction) {
                BoundNativeMethod(receiver, it)
            } else {
                it
            }
            stack[sp - argCount - 1] = callee
            call(callee, argCount)
            true
        } ?: invokeFromClass((receiver as? Instance)?.klass, name, argCount, checkError)
    }

    private fun invokeFromClass(klass: SClass?, name: String, argCount: Int, checkError: Boolean = true): Boolean {
        return (klass?.fields?.get(name) as? Closure)?.let { method ->
            callClosure(method, argCount)
            true
        } ?: run {
            if (checkError) {
                throw runtimeError("Undefined method $name.")
            } else {
                false
            }
        }
    }

    private fun invokeSuper(name: String, argCount: Int) {
        val superclass = peek(argCount) as SClass
        if (superclass.name == Constants.CLASS_OBJECT) {
            if (name == Constants.GET && argCount == 1) {
                getPropRaw(self, pop() as String)
                val result = pop()
                sp-- // pop superclass
                push(result)
                return
            } else if (name == Constants.SET && argCount == 2) {
                val value = pop()
                setPropRaw(self, pop(), value)
                sp-- // pop superclass
                return
            }
        }
        set(argCount, self!!) // bind the super method to self
        invokeFromClass(superclass, name, argCount)
        runLocal()
    }

    private fun captureUpvalue(sp: Int): Upvalue {
        var prevUpvalue: Upvalue? = null
        var upvalue = openUpvalues
        while (upvalue != null && upvalue.sp > sp) {
            prevUpvalue = upvalue
            upvalue = upvalue.next
        }
        if (upvalue != null && upvalue.sp == sp) {
            return upvalue
        }
        val createdUpvalue = Upvalue(WeakReference(sp), false, upvalue)
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
        return stack[upvalue.sp]!!
    }

    /**
     * @return true if the program should terminate
     */
    private fun doReturn(from: String, breakAtFp: Int): Boolean {
        val result = pop()
        val frameToReturnFrom = if (from.isEmpty()) frame.name else from
        var returningFrame: CallFrame
        do {
            returningFrame = frame
            closeUpvalues(returningFrame.sp)
            val numberOfPops = nextInt
            for (i in 0 until numberOfPops) {
                val code = nextCode
                when (code) { // TODO move somewhere else, reuse existing code
                    POP -> sp--
                    CLOSE_UPVALUE -> closeUpvalue()
                    POP_UNDER -> sp -= nextInt + 1 // + 1 is to pop the value on the stack as well
                    else -> throw IllegalArgumentException("Unexpected code in return scope closing patch: $code!")
                }
            }
            fp--
        } while (returningFrame.name != frameToReturnFrom)
        return if (fp == 0) { // Returning from top-level func
            sp = 0
            true
        } else {
            sp = returningFrame.sp
            push(result)
            fp == breakAtFp
        }
    }

    private fun inherit() {
        val superclass = pop()
        if (superclass !is SClass) {
            throw runtimeError("Superclass must be a class.")
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
                if (!field.key.startsWith(Constants.IMPLICIT)) { // Add public fields only
                    it.fields[field.key] = field.value
                }
            }
        }
    }

    private fun defineMethod(name: String) {
        val method = pop() as Closure
        val klass = peek() as SClass
        klass.fields[name] = method
    }

    private fun defineNativeMethod(name: String, method: NativeFunction) {
        val klass = peek() as SClass
        klass.fields[name] = method
    }

    private fun resizeStackIfNecessary() {
        if (sp == stackSize) {
            gc()
            stackSize *= STACK_GROWTH_FACTOR
            stack = stack.copyOf(stackSize)
        }
    }

    private fun push(o: Any) {
        resizeStackIfNecessary()
        stack[sp] = o
        sp++
    }

    private fun pop(): Any {
        sp--
        return stack[sp]!!
    }

    private fun peek(offset: Int = 0): Any {
        return stack[sp - offset - 1]!!
    }

    private fun set(offset: Int, value: Any) {
        stack[sp - offset - 1] = value
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
            sp--
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
        val loc = sp - offset - 1
        val value = stack[loc]
        if (value == Nil) {
            return Nil
        } else if (value is Instance || value is SClass) {
            return value as Fielded
        } else if (value is Long || value is Double) {
            val boxedNum = Instance(numClass!!).apply {
                fields[Constants.PRIVATE] = value
            }
            stack[loc] = boxedNum
            return boxedNum
        } else if (value is String) {
            val boxedStr = Instance(strClass!!).apply {
                fields[Constants.PRIVATE] = value
            }
            stack[loc] = boxedStr
            return boxedStr
        } else if (value is Closure) {
            val boxedClosure = Instance(funcClass!!).apply {
                fields[Constants.PRIVATE] = value
                fields[Constants.NAME] = value.function.name
                fields[Constants.ARITY] = value.function.arity
            }
            stack[loc] = boxedClosure
            return boxedClosure
        } else {
            throw runtimeError("Unable to box $value!")
        }
    }

    private fun gc() {
        for (i in sp until stackSize) {
            stack[i] = null
        }
        System.gc()
    }

    private fun printStack() {
        println(stack.copyOfRange(0, sp).joinToString(" "))
    }

    private fun runtimeError(message: String): Exception {
        println()
        println(message)
        printCallStack()
        return IllegalStateException()
    }

    private fun printCallStack() {
        for (i in fp - 1 downTo 0) {
            println(callFrames[i])
        }
    }

    private val buffer: ByteBuffer get() = frame.buffer
    private val currentFunction: Function get() = frame.closure.function
    private val self: Any? get() = stack[frame.sp] // self is always at 0

    private val nextCode: OpCode get() = OpCode.from(nextByte)
    private val nextByte: Byte get() = buffer.get()
    private val nextInt: Int get() = buffer.int
    private val nextLong: Long get() = buffer.long
    private val nextDouble: Double get() = buffer.double
    private val nextString: String get() = currentFunction.constants[nextInt] as String

    companion object {
        private const val INITIAL_STACK_SIZE = 1024
        private const val STACK_GROWTH_FACTOR = 4
        private const val MAX_FRAMES = 64
    }
}