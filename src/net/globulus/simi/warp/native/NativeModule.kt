package net.globulus.simi.warp.native

class NativeFunction(val arity: Int,
                     val func: (args: List<Any?>) -> Any?
) {
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