package net.globulus.simi.warp

class Function(
        val name: String,
        override val arity: Int,
        val upvalueCount: Int,
        val code: ByteArray,
        val constants: Array<Any>,
        val debugInfo: DebugInfo
) : OptionalParamsFunc {
    override var optionalParamsStart: Int = OptionalParamsFunc.DEFAULT_PARAMS_START
    override var defaultValues: Array<Any>? = null

    override fun toString(): String {
        return "<def $name $arity>"
    }
}