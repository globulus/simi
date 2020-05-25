package net.globulus.simi.warp

enum class OpCode {
    CONST_INT,
    CONST_FLOAT,
    CONST_ID,
    CONST,
    NIL,
    POP,
    POP_UNDER, // Keeps the top value and pops N values beneath it
    SET_LOCAL,
    GET_LOCAL,
    SET_UPVALUE,
    GET_UPVALUE,
    SET_PROP,
    GET_PROP,
    LT,
    LE,
    GT,
    GE,
    EQ,
    INVERT,
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
    JUMP_IF_NIL,
    CALL,
    CLOSURE,
    CLOSE_UPVALUE,
    RETURN,
    CLASS,
    METHOD,
    ;

    val byte = ordinal.toByte()

    companion object {
        fun from(byte: Byte) = values()[byte.toInt()]
    }
}