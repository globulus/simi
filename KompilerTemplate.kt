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

private var sp = 0
private var stackSize = INITIAL_STACK_SIZE
private var stack = arrayOfNulls<Any>(INITIAL_STACK_SIZE)

fun main(args: Array<String>) {
    INTERPRET
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