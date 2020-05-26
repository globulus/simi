package net.globulus.simi.warp

import net.globulus.simi.TokenType

object Nil {
    override fun toString(): String {
        return TokenType.NIL.toCode()
    }
}