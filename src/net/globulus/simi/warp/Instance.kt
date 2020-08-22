package net.globulus.simi.warp

import net.globulus.simi.Constants
import net.globulus.simi.TokenType

open class Instance(val klass: SClass) : Fielded {
    constructor(klass: SClass, mutable: Boolean) : this(klass) {
        this.mutable = mutable
    }

    var mutable = false
        internal set

    override val fields: MutableMap<String, Any> = mutableMapOf()

    operator fun get(key: String) = fields[key]

    operator fun set(key: String, value: Any) {
        if (value == Nil) {
            fields.remove(key)
        } else {
            fields[key] = value
        }
    }

    internal open fun stringify(vm: Vm): String {
        return StringBuilder()
                .append(if (mutable) TokenType.DOLLAR_LEFT_BRACKET.toCode() else TokenType.LEFT_BRACKET.toCode())
                .append(if (klass != Vm.declaredClasses[Constants.CLASS_OBJECT] && klass != Vm.declaredClasses[Constants.CLASS_LIST]) "class = ${klass.name}, " else "")
                .append(fields.entries
                        // for raw objects, just show their respective fields, ignoring the Object class methods
                        .filter { Vm.declaredClasses[Constants.CLASS_OBJECT]?.fields?.containsKey(it.key) == false }
                        .joinToString { "${it.key} ${TokenType.EQUAL.toCode()} ${vm.stringify(it.value)}" }
                )
                .append(TokenType.RIGHT_BRACKET.toCode())
                .toString()
    }

    override fun toString(): String {
        return "${klass.name} instance"
    }
}

class ListInstance(mutable: Boolean, providedItems: MutableList<Any>?) : Instance(Vm.declaredClasses[Constants.CLASS_LIST]!!, mutable) {
    internal val items = providedItems ?: mutableListOf()

    operator fun get(index: Int): Any {
        return if (index >= items.size) {
            Instance(Vm.declaredClasses[Constants.EXCEPTION_ILLEGAL_ARGUMENT]!!, false).apply {
                fields[Constants.MESSAGE] = "index: $index, size: ${items.size}"
            }
        } else if (index >= 0) {
            items[index]
        } else {
            items[items.size + index]
        }
    }

    operator fun set(index: Int, value: Any) {
        items[index] = value
    }

    operator fun plusAssign(item: Any) {
        items += item
    }

    internal fun sublist(from: Long, to: Long): Any {
        val len = items.size.toLong()
        val start = if (from < 0) len + from else from
        val end = if (to < 0) len + to else to
        if (start >= end) {
            return illegalArgumentException("start >= end, $start, $end")
        }
        if (start < 0) {
            return illegalArgumentException("start < 0, $start")
        }
        if (end > len) {
            return illegalArgumentException("end > len, $end, $len")
        }
        return ListInstance(mutable, items.subList(start.toInt(), end.toInt()))
    }

    override fun stringify(vm: Vm): String {
        return StringBuilder()
                .append(if (mutable) TokenType.DOLLAR_LEFT_BRACKET.toCode() else TokenType.LEFT_BRACKET.toCode())
                .append(items.joinToString { vm.stringify(it) })
                .append(TokenType.RIGHT_BRACKET.toCode())
                .toString()
    }
}