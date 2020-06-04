package net.globulus.simi.warp

import net.globulus.simi.Constants
import net.globulus.simi.TokenType

open class Instance(val klass: SClass, val mutable: Boolean) : Fielded {
    override val fields: MutableMap<String, Any> = mutableMapOf()

    override fun toString(): String {
        return when (klass) {
            Vm.objectClass -> {
                StringBuilder()
                        .append(if (mutable) TokenType.DOLLAR_LEFT_BRACKET.toCode() else TokenType.LEFT_BRACKET.toCode())
                        .append(fields.entries.joinToString { "${it.key} ${TokenType.EQUAL.toCode()} ${it.value}" })
                        .append(TokenType.RIGHT_BRACKET.toCode())
                        .toString()
            }
            else -> "${klass.name} instance"
        }
    }
}

class ListInstance(mutable: Boolean, providedItems: MutableList<Any>?) : Instance(Vm.listClass!!, mutable) {
    private val items = providedItems ?: mutableListOf()

    init {
        updateCount()
    }

    operator fun get(index: Int) = items[index]

    operator fun set(index: Int, value: Any) {
        items[index] = value
    }

    operator fun plusAssign(item: Any) {
        items += item
        updateCount()
    }

    private fun updateCount() {
        fields[Constants.COUNT] = items.size
    }

    override fun toString(): String {
        return StringBuilder()
                .append(if (mutable) TokenType.DOLLAR_LEFT_BRACKET.toCode() else TokenType.LEFT_BRACKET.toCode())
                .append(items.joinToString())
                .append(TokenType.RIGHT_BRACKET.toCode())
                .toString()
    }
}