package net.globulus.simi.warp

class Function(
        val name: String,
        val arity: Int,
        val upvalueCount: Int,
        val code: ByteArray,
        val constants: Array<Any>,
        val debugInfo: DebugInfo
) {
    internal var optionalParamsStart: Int = -1
    internal var defaultValues: Array<Any>? = null

    override fun toString(): String {
        return "<def $name $arity>"
    }
}