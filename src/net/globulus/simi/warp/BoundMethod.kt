package net.globulus.simi.warp

import net.globulus.simi.warp.native.NativeFunction

class BoundMethod(val receiver: Any,
                  val method: Closure
) {
    override fun toString(): String {
        return method.toString()
    }
}

class BoundNativeMethod(val receiver: Any,
                        val method: NativeFunction
) {
    override fun toString(): String {
        return method.toString()
    }
}