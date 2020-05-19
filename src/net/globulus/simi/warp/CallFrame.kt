package net.globulus.simi.warp

import java.nio.ByteBuffer

class CallFrame(val function: Function,
                val sp: Int
) {
    val buffer = ByteBuffer.wrap(function.code)

    private fun getCurrentLine(): Int {
        var line = 0
        val pos = buffer.position()
        for ((k, v) in function.debugInfo.lines.entries.sortedBy { it.key }) {
            if (v >= pos) {
                break
            }
            line = k
        }
        return line
    }

    override fun toString(): String {
        return "[line ${getCurrentLine()}] in ${function.name}"
    }
}