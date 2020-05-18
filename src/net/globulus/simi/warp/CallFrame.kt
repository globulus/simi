package net.globulus.simi.warp

import java.nio.ByteBuffer

class CallFrame(val function: Function,
                val sp: Int
) {
    val buffer = ByteBuffer.wrap(function.code)
}