package net.globulus.simi.warp

class Instance(val klass: SClass,
               val fields: MutableMap<String, Any> = mutableMapOf()
) {
    override fun toString(): String {
        return "${klass.name} instance"
    }
}