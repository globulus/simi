package net.globulus.simi.warp.debug

import net.globulus.simi.Token

class DebugInfo(val lines: Map<CodePointer, Int>, // Maps lines to chunk positions - line X starts at position X
                val locals: Map<Int, String>, // Maps stack pointers to local names
                val localsAtCodePoint: Map<CodePointer, Int>, // Maps code pointers to number of locals at their time
                val breakpoints: List<Int>, // List of bytecode positions that trigger breakpoints
                val tokens: List<Token>
)

data class CodePointer(val line: Int, val file: String) {
    override fun toString(): String {
        return "$file:$line"
    }
}