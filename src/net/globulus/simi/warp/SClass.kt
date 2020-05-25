package net.globulus.simi.warp

class SClass(val name: String,
             val fields: MutableMap<String, Any> = mutableMapOf()
) {
    override fun toString(): String {
        return name
    }
}