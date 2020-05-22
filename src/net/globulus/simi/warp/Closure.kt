package net.globulus.simi.warp

class Closure(val function: Function
) {
    val upvalues = arrayOfNulls<Upvalue>(function.upvalueCount)

    override fun toString(): String {
        return function.toString()
    }
}