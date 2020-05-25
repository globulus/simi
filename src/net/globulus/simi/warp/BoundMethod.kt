package net.globulus.simi.warp

class BoundMethod(val receiver: Any,
                  val method: Closure
) {
    override fun toString(): String {
        return method.toString()
    }
}