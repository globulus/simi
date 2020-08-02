package net.globulus.simi.warp.native

import net.globulus.simi.warp.OptionalParamsFunc

interface NativeFunc : OptionalParamsFunc

class NativeFunction(override val arity: Int,
                     val func: (args: List<Any?>) -> Any?
) : NativeFunc {
    override var optionalParamsStart: Int = OptionalParamsFunc.DEFAULT_PARAMS_START
    override var defaultValues: Array<Any>? = null

    override fun toString(): String {
        return "<native $arity>"
    }
}

class AsyncNativeFunction(override val arity: Int,
                          val func: (args: List<Any?>, callback: (Any?) -> Unit) -> Unit
) : NativeFunc {
    override var optionalParamsStart: Int = OptionalParamsFunc.DEFAULT_PARAMS_START
    override var defaultValues: Array<Any>? = null

    override fun toString(): String {
        return "<async native $arity>"
    }
}

interface NativeModule {
    val classes: Map<String, NativeClass>
}

interface NativeClass {
    fun resolve(funcName: String): NativeFunc?
}