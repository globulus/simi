package net.globulus.simi.warp

enum class OpCode {
    TRUE,
    FALSE,
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
    PROC,
    CLASS,
    INHERIT,
    MIXIN,
    METHOD,
    NATIVE_METHOD,
    INNER_CLASS,
    FIELD,
    CLASS_DECLR_DONE,
    SUPER,
    GET_SUPER,
    SUPER_INVOKE,
    SELF_FN,
    OBJECT,
    LIST,
    START_COMPREHENSION,
    ADD_TO_COMPREHENSION,
    ANNOTATE,
    ANNOTATE_FIELD, // has a separate call because class fields are only declared in the init, so we need to trigger the annotation process in the VM manually
    GET_ANNOTATIONS,
    GU,
    FIBER,
    YIELD,
    NIL_CHECK,
    ;

    val byte = ordinal.toByte()

    companion object {
        fun from(byte: Byte) = values()[byte.toInt()]
    }
}