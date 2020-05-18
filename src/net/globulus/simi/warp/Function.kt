package net.globulus.simi.warp

class Function(
        val name: String,
        val arity: Int,
        val code: ByteArray,
        val constants: Array<Any>,
        val overloads: Map<Int, Any>? = null
)