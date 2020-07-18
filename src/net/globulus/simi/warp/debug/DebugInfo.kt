package net.globulus.simi.warp.debug

import net.globulus.simi.Token
import net.globulus.simi.warp.Compiler

class DebugInfo(val compiler: Compiler,
                val lifetime: Lifetime, // which codepoint did the function def start and end when does it end
                val locals: List<Pair<Compiler.Local, Lifetime>>, // list of all the locals ever and when were they discarded (at which line)
                val lines: Map<CodePointer, Int>, // Maps lines to chunk positions - line X starts at position X
                val breakpoints: List<Int> // List of bytecode positions that trigger breakpoints
)

data class CodePointer(val line: Int, val file: String) {

    constructor(token: Token) : this(token.line, token.file)

    override fun toString(): String {
        return "$file:$line"
    }

    companion object {
        val EMPTY = CodePointer(-1, "")
        val UNKNOWN = CodePointer(-1, "Unknown (enable debug mode)")
    }
}

fun Token.codePointer() = CodePointer(this)

data class Lifetime(val start: CodePointer, var end: CodePointer?) {

    constructor(startToken: Token, endToken: Token?)
            : this(CodePointer(startToken), if (endToken != null) CodePointer(endToken) else null)

    companion object {
        val EMPTY = Lifetime(CodePointer.EMPTY, CodePointer.EMPTY)
        fun of(tokens: List<Token>): Lifetime {
            if (tokens.isEmpty()) {
                return EMPTY
            }
            return Lifetime(tokens[0], tokens[tokens.size - 1])
        }
    }
}