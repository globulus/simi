package net.globulus.simi.warp

class SClass(val name: String) : Fielded {
    override val fields: MutableMap<String, Any> = mutableMapOf()
    val superclasses: MutableMap<String, SClass> = mutableMapOf()

    fun checkIs(other: SClass): Boolean {
        if (this == other) {
            return true
        }
        for (superclass in superclasses.values) {
            if (superclass == other) {
                return true
            }
            if (superclass.checkIs(other)) {
                return true
            }
        }
        return false
    }

    override fun toString(): String {
        return name
    }
}