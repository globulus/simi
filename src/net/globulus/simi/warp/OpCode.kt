package net.globulus.simi.warp

enum class OpCode {
    CONST_INT,
    CONST_FLOAT,
    CONST_ID,
    CONST,
    CONST_OUTER,
    NIL,
    POP,
    SET_LOCAL,
    GET_LOCAL,
    SET_OUTER,
    GET_OUTER,
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
    RETURN,
    ;

    val byte = ordinal.toByte()

    companion object {
        fun from(byte: Byte) = values()[byte.toInt()]
    }
}