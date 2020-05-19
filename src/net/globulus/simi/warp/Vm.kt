package net.globulus.simi.warp

import net.globulus.simi.warp.OpCode.*
import java.nio.ByteBuffer
import kotlin.math.round

internal class Vm {
    private var sp = 0
    private var stackSize = INITIAL_STACK_SIZE
    private var stack = arrayOfNulls<Any>(INITIAL_STACK_SIZE)

    private var fp = 0
        set(value) {
            field = value
            if (value > 0) {
                frame = callFrames[value - 1]!!
            }
        }
    private lateinit var frame: CallFrame
    private val callFrames = arrayOfNulls<CallFrame>(MAX_FRAMES)

    fun interpret(input: Function) {
        push(input)
        call(input, 0)
        try {
            run()
            printStack()
        } catch (ignored: IllegalStateException) { } // Silently abort the program
    }

    private fun run() {
        loop@ while (true) {
            val code = nextCode
            when (code) {
                CONST_INT -> push(nextLong)
                CONST_FLOAT -> push(nextDouble)
                CONST_ID -> throw runtimeError("WTF")
                CONST -> push(frame.function.constants[nextInt])
                NIL -> push(Nil)
                POP -> sp--
                SET_LOCAL -> stack[frame.sp + nextInt] = pop()
                GET_LOCAL -> push(stack[frame.sp + nextInt]!!)
                NEGATE -> negate()
                ADD -> add()
                SUBTRACT, MULTIPLY, DIVIDE, DIVIDE_INT, MOD, LE, LT, GE, GT -> binaryOpOnStack(code)
                EQ, NE -> checkEquality(code)
                PRINT -> println(pop())
                JUMP -> buffer.position(nextInt)
                JUMP_IF_FALSE -> {
                    val offset = nextInt
                    if (isFalsey(peek())) {
                        buffer.position(offset)
                    }
                }
                CALL -> {
                    val argCount = nextInt
                    call(peek(argCount), argCount)
                }
                RETURN -> {
                    val result = pop()
                    val returningFrame = frame
                    fp--
                    if (fp == 0) { // Returning from top-level func
                        sp = 0
                        break@loop
                    }
                    sp = returningFrame.sp
                    push(result)
                }
            }
        }
    }

    private fun isFalsey(o: Any): Boolean {
        return when (o) {
            Nil -> true
            is Long -> o == 0L
            is Double -> o == 0.0
            else -> false
        }
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
            else -> binaryOp(ADD, a, b)
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
        // TODO add comparison by equals()
        return a == b
    }

    private fun call(callee: Any, argCount: Int) {
        if (callee !is Function) {
            throw runtimeError("Callee is not a func!")
        }
        if (argCount != callee.arity) {
            if (callee.optionalParamsStart != -1 && argCount >= callee.optionalParamsStart) {
                for (i in argCount until (callee.optionalParamsStart + callee.defaultValues!!.size)) {
                    push(callee.defaultValues!![i - callee.optionalParamsStart])
                }
            } else {
                throw runtimeError("Expected ${callee.arity} arguments but got $argCount.")
            }
        }
        if (fp == MAX_FRAMES) {
            throw runtimeError("Stack overflow.")
        }
        callFrames[fp] = CallFrame(callee, sp - callee.arity - 1)
        fp++
    }

    private fun resizeStackIfNecessary() {
        if (sp == stackSize) {
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

    private val nextCode: OpCode get() = OpCode.from(nextByte)
    private val nextByte: Byte get() = buffer.get()
    private val nextInt: Int get() = buffer.int
    private val nextLong: Long get() = buffer.long
    private val nextDouble: Double get() = buffer.double

    companion object {
        private const val INITIAL_STACK_SIZE = 1024
        private const val STACK_GROWTH_FACTOR = 4
        private const val MAX_FRAMES = 64
    }
}