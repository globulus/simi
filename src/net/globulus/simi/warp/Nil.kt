package net.globulus.simi.warp

import net.globulus.simi.TokenType

object Nil : Fielded {
    override val fields = mutableMapOf<String, Any>()

    override fun toString(): String {
        return TokenType.NIL.toCode()
    }
}