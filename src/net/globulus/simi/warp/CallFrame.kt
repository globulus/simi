package net.globulus.simi.warp

import net.globulus.simi.warp.debug.CodePointer
import java.nio.ByteBuffer

class CallFrame(val closure: Closure,
                val sp: Int
) {
    val buffer: ByteBuffer = ByteBuffer.wrap(closure.function.code)
    val name = closure.function.name

    internal fun getCurrentCodePoint(): CodePointer {
        return closure.function.debugInfo?.let {
            var pointer: CodePointer? = null
            val pos = buffer.position()
            for ((k, v) in closure.function.debugInfo.lines.entries.sortedBy { it.value }) {
                if (v > pos) {
                    if (pointer == null) {
                        pointer = k
                    }
                    break
                }
                pointer = k
            }
            pointer!!
        } ?: CodePointer.UNKNOWN
    }

    override fun toString(): String {
        return "[${getCurrentCodePoint()}] in ${closure.function.name}"
    }
}