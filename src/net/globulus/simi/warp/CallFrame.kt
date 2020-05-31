package net.globulus.simi.warp

import java.nio.ByteBuffer

class CallFrame(val closure: Closure,
                val sp: Int
) {
    val buffer: ByteBuffer = ByteBuffer.wrap(closure.function.code)
    val name = closure.function.name

    private fun getCurrentLine(): Int {
        var line = 0
        val pos = buffer.position()
        for ((k, v) in closure.function.debugInfo.lines.entries.sortedBy { it.key }) {
            if (v >= pos) {
                break
            }
            line = k
        }
        return line
    }

    override fun toString(): String {
        return "[line ${getCurrentLine()}] in ${closure.function.name}"
    }
}