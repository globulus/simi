package net.globulus.simi

import java.nio.ByteBuffer
import kotlin.math.round

object Nil

private const val INITIAL_STACK_SIZE = 1024
private const val STACK_GROWTH_FACTOR = 4

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
    HALT,
    ;

    val byte = ordinal.toByte()

    companion object {
        fun from(byte: Byte) = values()[byte.toInt()]
    }
}

private val buffer = ByteBuffer.wrap(byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 5, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 15, 7, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 4, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 2, 17, 15, 18, 16, 6, 0, 0, 0, 0, 7, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 2, 18, 6, 0, 0, 0, 1, 3, 0, 0, 0, 2, 7, 0, 0, 0, 0, 15, 3, 0, 0, 0, 3, 15, 21, 3, 0, 0, 0, 4, 7, 0, 0, 0, 1, 15, 3, 0, 0, 0, 5, 15, 21, 3, 0, 0, 0, 6, 21, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 8, 23, 0, 0, 0, -95, 22, 0, 0, 0, -79, 5, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5, 10, 23, 0, 0, 0, -62, 5, 3, 0, 0, 0, 0, 21, 22, 0, 0, 0, -22, 5, 7, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 4, 12, 23, 0, 0, 0, -29, 5, 3, 0, 0, 0, 1, 21, 22, 0, 0, 0, -22, 5, 3, 0, 0, 0, 7, 21, 0, 0, 0, 0, 0, 0, 0, 0, 5, 6, 0, 0, 0, 0, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 10, 23, 0, 0, 1, -110, 5, 3, 0, 0, 0, 2, 7, 0, 0, 0, 0, 15, 3, 0, 0, 0, 8, 15, 21, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 12, 23, 0, 0, 1, 82, 5, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 16, 6, 0, 0, 0, 0, 22, 0, 0, 0, -8, 22, 0, 0, 1, 115, 5, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 12, 23, 0, 0, 1, 114, 5, 22, 0, 0, 1, -109, 22, 0, 0, 1, 115, 5, 3, 0, 0, 0, 9, 21, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 16, 6, 0, 0, 0, 0, 22, 0, 0, 0, -8, 5, 3, 0, 0, 0, 10, 7, 0, 0, 0, 0, 15, 3, 0, 0, 0, 8, 15, 21, 3, 0, 0, 0, 11, 21, 0, 0, 0, 0, 0, 0, 0, 0, 5, 6, 0, 0, 0, 0, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 12, 23, 0, 0, 1, -39, 5, 3, 0, 0, 0, 12, 21, 22, 0, 0, 2, 27, 5, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 12, 23, 0, 0, 1, -13, 22, 0, 0, 2, 3, 5, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 12, 23, 0, 0, 2, 20, 5, 3, 0, 0, 0, 13, 21, 22, 0, 0, 2, 27, 5, 3, 0, 0, 0, 14, 21, 5, 5, 24))
private val strings = arrayOf("a", "b", "a is ", " == 7.33", "b is ", " == 4", "if test \"a\" ==", "both are false", "", "again", "while test 2 == ", "when test 2 == ", "2", "3 or 4", "else")
private var sp = 0
private var stackSize = INITIAL_STACK_SIZE
private var stack = arrayOfNulls<Any>(INITIAL_STACK_SIZE)

fun main(args: Array<String>) {
    interpret()
}

fun interpret() {
    loop@ while (true) {
        val code = nextCode
        when (code) {
            OpCode.CONST_INT -> push(nextLong)
            OpCode.CONST_FLOAT -> push(nextDouble)
            OpCode.CONST_ID -> throw RuntimeException("WTF")
            OpCode.CONST_STR -> push(strings[nextInt])
            OpCode.NIL -> push(Nil)
            OpCode.POP -> sp--
            OpCode.SET_LOCAL -> stack[nextInt] = pop()
            OpCode.GET_LOCAL -> push(stack[nextInt]!!)
            OpCode.NEGATE -> negate()
            OpCode.ADD -> add()
            OpCode.SUBTRACT, OpCode.MULTIPLY, OpCode.DIVIDE, OpCode.DIVIDE_INT, OpCode.MOD, OpCode.LE, OpCode.LT, OpCode.GE, OpCode.GT -> binaryOpOnStack(code)
            OpCode.EQ, OpCode.NE -> checkEquality(code)
            OpCode.PRINT -> println(pop())
            OpCode.JUMP -> buffer.position(nextInt)
            OpCode.JUMP_IF_FALSE -> {
                val offset = nextInt
                if (isFalsey(peek())) {
                    buffer.position(offset)
                }
            }
            OpCode.HALT -> break@loop
        }
    }
    printStack()
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
        else -> binaryOp(OpCode.ADD, a, b)
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
        OpCode.ADD -> a + b
        OpCode.SUBTRACT -> a - b
        OpCode.MULTIPLY -> a * b
        OpCode.DIVIDE -> intIfPossible(a * 1.0 / b)
        OpCode.DIVIDE_INT -> a / b
        OpCode.MOD -> a % b
        OpCode.LT -> boolToLong(a < b)
        OpCode.LE -> boolToLong(a <= b)
        OpCode.GE -> boolToLong(a >= b)
        OpCode.GT -> boolToLong(a > b)
        else -> throw IllegalArgumentException("WTF")
    }
}

private fun binaryOpTwoDoubles(opCode: OpCode, a: Double, b: Double): Any {
    return when (opCode) {
        OpCode.ADD -> intIfPossible(a + b)
        OpCode.SUBTRACT -> intIfPossible(a - b)
        OpCode.MULTIPLY -> intIfPossible(a * b)
        OpCode.DIVIDE -> intIfPossible(a / b)
        OpCode.MOD ->intIfPossible(a % b)
        OpCode.LT -> boolToLong(a < b)
        OpCode.LE -> boolToLong(a <= b)
        OpCode.GE -> boolToLong(a >= b)
        OpCode.GT -> boolToLong(a > b)
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
    push(boolToLong(if (code == OpCode.EQ) r else !r))
}

private fun areEqual(a: Any, b: Any): Boolean {
    // TODO add comparison by equals()
    return a == b
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

private val nextCode: OpCode get() = OpCode.from(nextByte)
private val nextByte: Byte get() = buffer.get()
private val nextInt: Int get() = buffer.int
private val nextLong: Long get() = buffer.long
private val nextDouble: Double get() = buffer.double