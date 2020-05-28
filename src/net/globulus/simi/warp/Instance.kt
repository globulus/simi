package net.globulus.simi.warp

class Instance(val klass: SClass) : Fielded {
    override val fields: MutableMap<String, Any> = mutableMapOf()

    override fun toString(): String {
        return "${klass.name} instance"
    }
}