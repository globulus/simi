package net.globulus.simi.warp.debug

import net.globulus.simi.warp.Compiler

class DebugInfo(val compiler: Compiler,
                val locals: List<Pair<Compiler.Local, LocalLifetime>>, // list of all the locals ever and when were they discarded (at which line)
                val lines: Map<CodePointer, Int>, // Maps lines to chunk positions - line X starts at position X
                val breakpoints: List<Int> // List of bytecode positions that trigger breakpoints
)

data class CodePointer(val line: Int, val file: String) {
    override fun toString(): String {
        return "$file:$line"
    }
}

data class LocalLifetime(val start: CodePointer, var end: CodePointer?)