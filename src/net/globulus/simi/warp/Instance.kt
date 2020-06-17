package net.globulus.simi.warp

import net.globulus.simi.Constants
import net.globulus.simi.TokenType

open class Instance(val klass: SClass, val mutable: Boolean) : Fielded {
    override val fields: MutableMap<String, Any> = mutableMapOf()

    operator fun get(key: String) = fields[key]

    operator fun set(key: String, value: Any) {
        if (value == Nil) {
            fields.remove(key)
        } else {
            fields[key] = value
        }
        updateCount()
    }

    internal open fun updateCount() {
        fields[Constants.COUNT] = fields.size
    }

    internal open fun stringify(vm: Vm): String {
        return StringBuilder()
                .append(if (mutable) TokenType.DOLLAR_LEFT_BRACKET.toCode() else TokenType.LEFT_BRACKET.toCode())
                .append(fields.entries
                        // for raw objects, just show their respective fields, ignoring the Object class methods
                        .filter { Vm.objectClass?.fields?.containsKey(it.key) == false }
                        .joinToString { "${it.key} ${TokenType.EQUAL.toCode()} ${vm.stringify(it.value)}" }
                )
                .append(TokenType.RIGHT_BRACKET.toCode())
                .toString()
    }

    override fun toString(): String {
        return "${klass.name} instance"
    }
}

class ListInstance(mutable: Boolean, providedItems: MutableList<Any>?) : Instance(Vm.listClass!!, mutable) {
    internal val items = providedItems ?: mutableListOf()

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

    override fun updateCount() {
        fields[Constants.COUNT] = items.size
    }

    override fun stringify(vm: Vm): String {
        return StringBuilder()
                .append(if (mutable) TokenType.DOLLAR_LEFT_BRACKET.toCode() else TokenType.LEFT_BRACKET.toCode())
                .append(items.joinToString { vm.stringify(it) })
                .append(TokenType.RIGHT_BRACKET.toCode())
                .toString()
    }
}