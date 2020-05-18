package net.globulus.simi.warp

import net.globulus.simi.warp.OpCode.*
import java.nio.ByteBuffer
import kotlin.math.round

internal class Vm {
    private var sp = 0
    private var stackSize = INITIAL_STACK_SIZE
    private var stack = arrayOfNulls<Any>(INITIAL_STACK_SIZE)

    private var fp = 0
    private lateinit var frame: CallFrame
    private val callFrames = mutableListOf<CallFrame>()

    fun interpret(input: Function) {
        push(input)
        call(input)
        printStack()
    }

    private fun run() {
        loop@ while (true) {
            val code = nextCode
            when (code) {
                CONST_INT -> push(nextLong)
                CONST_FLOAT -> push(nextDouble)
                CONST_ID -> throw RuntimeException("WTF")
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
                CALL -> call(pop())
                RETURN, HALT -> break@loop
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
            else -> throw IllegalArgumentException("WTF")
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
            else -> throw IllegalArgumentException("WTF")
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

    private fun call(callee: Any) {
        if (callee !is Function) {
            throw RuntimeException("Callee is not a func!")
        }
        callFrames += CallFrame(callee, sp - 1).apply {
            frame = this
        }
        run()
        frame = callFrames[callFrames.size - 2]
        sp = frame.sp
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

    private fun peek(): Any {
        return stack[sp - 1]!!
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

    private val buffer: ByteBuffer get() = frame.buffer

    private val nextCode: OpCode get() = OpCode.from(nextByte)
    private val nextByte: Byte get() = buffer.get()
    private val nextInt: Int get() = buffer.int
    private val nextLong: Long get() = buffer.long
    private val nextDouble: Double get() = buffer.double

    object Nil

    companion object {
        private const val INITIAL_STACK_SIZE = 1024
        private const val STACK_GROWTH_FACTOR = 4
        private const val MAX_FRAMES = 64
    }
}