package net.globulus.simi.warp

class Instance(val klass: SClass,
               override val fields: MutableMap<String, Any> = mutableMapOf()
) : Fielded {
    override fun toString(): String {
        return "${klass.name} instance"
    }
}