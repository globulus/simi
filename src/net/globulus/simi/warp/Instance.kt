package net.globulus.simi.warp

import net.globulus.simi.TokenType

class Instance(val klass: SClass, val mutable: Boolean) : Fielded {
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
            Vm.listClass -> {
                StringBuilder()
                        .append(if (mutable) TokenType.DOLLAR_LEFT_BRACKET.toCode() else TokenType.LEFT_BRACKET.toCode())
                        .append(fields.entries.sortedBy { it.key }.joinToString { "${it.value}" })
                        .append(TokenType.RIGHT_BRACKET.toCode())
                        .toString()
            }
            else -> "${klass.name} instance"
        }
    }
}