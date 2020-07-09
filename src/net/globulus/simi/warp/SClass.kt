package net.globulus.simi.warp

import net.globulus.simi.Constants
import net.globulus.simi.warp.native.NativeFunction

class SClass(val name: String, val kind: Kind) : Fielded {
    override val fields = mutableMapOf<String, Any>()
    val superclasses = mutableMapOf<String, SClass>()
    val annotations = mutableMapOf<String, Array<Any>>()

    // TODO maybe add overridden toString here?
    var overriddenGet: Any? = null
        private set
    var overriddenSet: Any? = null
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
        overriddenGet = if (getArity == 1) {
            getMethod
        } else {
            superclasses.values.firstOrNull { it.overriddenGet != null }?.overriddenGet
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
        overriddenSet = if (setArity == 2) {
            setMethod
        } else {
            superclasses.values.firstOrNull { it.overriddenSet != null }?.overriddenSet
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

    val mergedAnnotations: MutableMap<String, Array<Any>> get() {
        val merge = mutableMapOf<String, Array<Any>>()
        for (superclass in superclasses.values) {
            merge.putAll(superclass.mergedAnnotations)
        }
        merge.putAll(annotations)
        return merge
    }

    override fun toString(): String {
        return name
    }

    enum class Kind {
        MODULE, ENUM, META, FINAL, REGULAR, OPEN;

        val byte = ordinal.toByte()

        companion object {
            fun from(byte: Byte) = values()[byte.toInt()]
        }
    }
}