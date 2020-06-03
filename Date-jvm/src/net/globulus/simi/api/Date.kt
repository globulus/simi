package net.globulus.simi.api

import net.globulus.simi.warp.Instance
import net.globulus.simi.warp.SClass
import net.globulus.simi.warp.native.NativeClass
import net.globulus.simi.warp.native.NativeFunction
import net.globulus.simi.warp.native.NativeModule
import java.util.Date

class Date : NativeModule {
    override val classes: Map<String, NativeClass> = mapOf(
            "List" to object : NativeClass {
                override fun resolve(funcName: String): NativeFunction? {
                    return when (funcName) {
                        "get" -> NativeFunction(1) {
                            val instance = it[0] as Instance
                            val key = it[1] as Int
                            instance.fields[key.toString()]
                        }
                        "set" -> NativeFunction(2) {
                            val instance = it[0] as Instance
                            val key = it[1] as Int
                            val value = it[2] as Any
                            instance.fields[key.toString()] = value
                            null
                        }
                        else -> null
                    }
                }
            },
            "Date" to object : NativeClass {
                override fun resolve(funcName: String): NativeFunction? {
                    return when (funcName) {
                        "init" -> NativeFunction(0) {
                            val instance = it[0] as Instance
                            val date = Date()
                            instance.apply {
                                fields["_"] = date
                                fields["time"] = date.time
                            }
                        }
                        "at" -> NativeFunction(1) {
                            val klass = it[0] as SClass
                            val time = it[1] as Long
                            val date = Date(time)
                            Instance(klass, false).apply {
                                fields["_"] = date
                                fields["time"] = date.time
                            }
                        }
                        else -> null
                    }
                }

            }
    )
}