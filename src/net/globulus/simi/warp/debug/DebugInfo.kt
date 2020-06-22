package net.globulus.simi.warp.debug

import net.globulus.simi.Token

class DebugInfo(val lines: Map<CodePointer, Int>, // Maps lines to chunk positions - line X starts at position X
                val locals: Map<Int, String>, // Maps stack pointers to local names
                val breakpoints: List<Int>, // List of bytecode positions that trigger breakpoints
                val breakpointFiles: List<String>, // names of files for all the breakpoints
                val tokens: List<Token>
)

data class CodePointer(val line: Int, val file: String) {
    override fun toString(): String {
        return "$file:$line"
    }
}