package net.globulus.simi.warp

enum class OpCode {
    CONST_INT,
    CONST_FLOAT,
    CONST_ID,
    CONST,
    NIL,
    POP,
    POP_UNDER, // Keeps the top value and pops N values beneath it
    DUPLICATE, // Duplicates the value at the top of the stack
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
    IS,
    INVERT,
    NEGATE,
    ADD,
    SUBTRACT,
    MULTIPLY,
    DIVIDE,
    DIVIDE_INT,
    MOD,
    HAS, // Requires a special OpCode as the operands are inverted
    PRINT,
    JUMP,
    JUMP_IF_FALSE,
    JUMP_IF_NIL,
    JUMP_IF_EXCEPTION,
    CALL,
    INVOKE,
    CLOSURE,
    CLOSE_UPVALUE,
    RETURN,
    CLASS,
    INHERIT,
    IMPORT,
    METHOD,
    NATIVE_METHOD,
    INNER_CLASS,
    CLASS_DECLR_DONE,
    SUPER,
    GET_SUPER,
    SUPER_INVOKE,
    SELF_DEF,
    OBJECT,
    LIST,
    ANNOTATE,
    GET_ANNOTATIONS,
    ;

    val byte = ordinal.toByte()

    companion object {
        fun from(byte: Byte) = values()[byte.toInt()]
    }
}