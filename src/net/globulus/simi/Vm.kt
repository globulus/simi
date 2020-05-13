package net.globulus.simi

import net.globulus.simi.Compiler.OpCode
import java.nio.ByteBuffer
import java.util.*
import kotlin.math.round

internal class Vm {
    private lateinit var buffer: ByteBuffer
    private var sp = 0
    private var stackSize = INITIAL_STACK_SIZE
    private var stack = arrayOfNulls<Any>(INITIAL_STACK_SIZE)
    private var scopeStarts = Stack<Int>() // At which point in the stack does a certain scope depth start

    fun interpret(input: Compiler.CompilerOutput) {
        buffer = ByteBuffer.wrap(input.byteCode)
        loop@ while (true) {
            val code = nextCode
            when (code) {
                OpCode.CONST_INT -> push(nextLong)
                OpCode.CONST_FLOAT -> push(nextDouble)
                OpCode.CONST_ID -> throw RuntimeException("WTF")
                OpCode.CONST_STR -> push(input.strings[nextInt])
                OpCode.NIL -> throw RuntimeException("WTF")
                OpCode.POP -> pop()
                OpCode.PUSH_SCOPE -> scopeStarts.push(sp)
                OpCode.POP_SCOPE -> sp = scopeStarts.pop()
                OpCode.SET_LOCAL -> stack[nextInt] = pop()
                OpCode.GET_LOCAL -> push(stack[nextInt]!!)
                OpCode.NEGATE -> negate()
                OpCode.ADD -> add()
                OpCode.SUBTRACT -> subtract()
                OpCode.MULTIPLY -> multiply()
                OpCode.DIVIDE -> divide()
                OpCode.DIVIDE_INT -> divideInt()
                OpCode.MOD -> mod()
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
        when (val a = pop()) {
            is Long -> when (b) {
                is Long -> push(a + b)
                is Double -> push(a + b)
                is String -> push(a.toString() + b)
            }
            is Double -> when (b) {
                is Long -> push(a + b)
                is Double -> push(a + b)
                is String -> push(a.toString() + b)
            }
            is String -> push(a + b)
        }
    }

    private fun subtract() {
        val b = pop()
        val a = pop()
        if (a is Long) {
            if (b is Long) {
                push(a - b)
            } else if (b is Double) {
                push(a - b)
            }
        } else if (a is Double) {
            if (b is Long) {
                push(a - b)
            } else if (b is Double) {
                push(a - b)
            }
        }
    }

    private fun multiply() {
        val b = pop()
        val a = pop()
        if (a is Long) {
            if (b is Long) {
                push(a * b)
            } else if (b is Double) {
                push(a * b)
            }
        } else if (a is Double) {
            if (b is Long) {
                push(a * b)
            } else if (b is Double) {
                push(a * b)
            }
        }
    }

    private fun divide() {
        val b = pop()
        val a = pop()
        if (a is Long) {
            if (b is Long) {
                push(a.toDouble() / b)
            } else if (b is Double) {
                push(a / b)
            }
        } else if (a is Double) {
            if (b is Long) {
                push(a / b)
            } else if (b is Double) {
                push(a / b)
            }
        }
    }

    private fun divideInt() {
        val b = pop()
        val a = pop()
        if (a is Long) {
            if (b is Long) {
                push(a / b)
            } else if (b is Double) {
                push(a / round(b))
            }
        } else if (a is Double) {
            if (b is Long) {
                push(round(a) / b)
            } else if (b is Double) {
                push(round(a) / round(b))
            }
        }
    }

    private fun mod() {
        val b = pop()
        val a = pop()
        if (a is Long) {
            if (b is Long) {
                push(a % b)
            } else if (b is Double) {
                push(a % b)
            }
        } else if (a is Double) {
            if (b is Long) {
                push(a % b)
            } else if (b is Double) {
                push(a % b)
            }
        }
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

    private val nextCode: OpCode get() = Compiler.OpCode.from(nextByte)
    private val nextByte: Byte get() = buffer.get()
    private val nextInt: Int get() = buffer.int
    private val nextLong: Long get() = buffer.long
    private val nextDouble: Double get() = buffer.double

    companion object {
        private const val INITIAL_STACK_SIZE = 1024
        private const val STACK_GROWTH_FACTOR = 4
    }
}