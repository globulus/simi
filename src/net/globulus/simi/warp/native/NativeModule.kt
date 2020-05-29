package net.globulus.simi.warp.native

import net.globulus.simi.warp.OptionalParamsFunc

class NativeFunction(override val arity: Int,
                     val func: (args: List<Any?>) -> Any?
) : OptionalParamsFunc {
    override var optionalParamsStart: Int = OptionalParamsFunc.DEFAULT_PARAMS_START
    override var defaultValues: Array<Any>? = null

    override fun toString(): String {
        return "<native $arity>"
    }
}

interface NativeModule {
    val classes: Map<String, NativeClass>
}

interface NativeClass {
    fun resolve(funcName: String): NativeFunction?
}