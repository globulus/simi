package net.globulus.simi.api

interface KotlinCodifiable<T>{
    fun toKotlinCode(vararg args: Any): T
}