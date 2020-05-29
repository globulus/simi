package net.globulus.simi.warp

internal interface OptionalParamsFunc {
    val arity: Int
    var optionalParamsStart: Int
    var defaultValues: Array<Any>?

    companion object {
        const val DEFAULT_PARAMS_START = -1
    }
}