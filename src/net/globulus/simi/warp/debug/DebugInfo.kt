package net.globulus.simi.warp.debug

import net.globulus.simi.Token

class DebugInfo(val lines: Map<Int, Int>, // Maps lines to chunk positions - line X starts at position X
                val locals: Map<Int, String>, // Maps stack pointers to local names
                val breakpoints: List<Int>, // List of bytecode positions that trigger breakpoints
                val tokens: List<Token>
)