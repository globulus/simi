package net.globulus.simi.warp

enum class OpCode {
    CONST_INT,
    CONST_FLOAT,
    CONST_ID,
    CONST,
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
    CALL,
    RETURN,
    HALT,
    ;

    val byte = ordinal.toByte()

    companion object {
        fun from(byte: Byte) = values()[byte.toInt()]
    }
}