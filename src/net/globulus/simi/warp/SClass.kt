package net.globulus.simi.warp

import net.globulus.simi.Constants
import net.globulus.simi.warp.native.NativeFunction

class SClass(val name: String, val kind: Kind) : Fielded {
    override val fields: MutableMap<String, Any> = mutableMapOf()
    val superclasses: MutableMap<String, SClass> = mutableMapOf()

    var overridenGet: Any? = null
        private set
    var overridenSet: Any? = null
        private set

    fun finalizeDeclr() {
        val getMethod = when (val getField = fields[Constants.GET]) {
            is Closure, is NativeFunction -> getField
            else -> null
        }
        val getArity = when (getMethod) {
            is Closure -> getMethod.function.arity
            is NativeFunction -> getMethod.arity
            else -> null
        }
        overridenGet = if (getArity == 1) {
            getMethod
        } else {
            superclasses.values.firstOrNull { it.overridenGet != null }?.overridenGet
        }
        val setMethod = when (val setField = fields[Constants.SET]) {
            is Closure, is NativeFunction -> setField
            else -> null
        }
        val setArity = when (setMethod) {
            is Closure -> setMethod.function.arity
            is NativeFunction -> setMethod.arity
            else -> null
        }
        overridenSet = if (setArity == 2) {
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

    enum class Kind {
        FINAL, REGULAR, OPEN;

        val byte = ordinal.toByte()

        companion object {
            fun from(byte: Byte) = values()[byte.toInt()]
        }
    }
}