package net.globulus.simi.warp

import net.globulus.simi.Constants

class SClass(val name: String) : Fielded {
    override val fields: MutableMap<String, Any> = mutableMapOf()
    val superclasses: MutableMap<String, SClass> = mutableMapOf()

    var overridenGet: Closure? = null
        private set
    var overridenSet: Closure? = null
        private set

    fun finalizeDeclr() {
        val getMethod = fields[Constants.GET] as? Closure
        overridenGet = if (getMethod?.function?.arity == 1) {
            getMethod
        } else {
            superclasses.values.firstOrNull { it.overridenGet != null }?.overridenGet
        }
        val setMethod = fields[Constants.SET] as? Closure
        overridenSet = if (setMethod?.function?.arity == 2) {
            setMethod
        } else {
            superclasses.values.firstOrNull { it.overridenSet != null }?.overridenSet
        }
    }

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