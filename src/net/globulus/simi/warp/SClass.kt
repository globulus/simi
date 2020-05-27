package net.globulus.simi.warp

class SClass(val name: String) : Fielded {
    override val fields: MutableMap<String, Any> = mutableMapOf()
    val superclasses: MutableMap<String, SClass> = mutableMapOf()

    override fun toString(): String {
        return name
    }
}