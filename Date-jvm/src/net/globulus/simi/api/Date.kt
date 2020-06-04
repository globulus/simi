package net.globulus.simi.api

import net.globulus.simi.Constants
import net.globulus.simi.warp.Instance
import net.globulus.simi.warp.ListInstance
import net.globulus.simi.warp.SClass
import net.globulus.simi.warp.Vm
import net.globulus.simi.warp.native.NativeClass
import net.globulus.simi.warp.native.NativeFunction
import net.globulus.simi.warp.native.NativeModule
import java.util.Date

class Date : NativeModule {
    override val classes: Map<String, NativeClass> = mapOf(
            "List" to object : NativeClass {
                override fun resolve(funcName: String): NativeFunction? {
                    return when (funcName) {
                        Constants.INIT -> NativeFunction(0) {
                            ListInstance(true, null)
                        }
                        Constants.GET -> NativeFunction(1) {
                            val instance = it[0] as ListInstance
                            when (val key = it[1]) {
                                is Long -> instance[key.toInt()]
                                is String -> instance.fields[key]
                                else -> null
                            }
                        }
                        Constants.SET -> NativeFunction(2) {
                            val instance = it[0] as ListInstance
                            val key = it[1] as Long
                            val value = it[2] as Any
                            instance[key.toInt()] = value
                            null
                        }
                        "add" -> NativeFunction(1) {
                            val instance = it[0] as ListInstance
                            val value = it[1] as Any
                            instance += value
                            null
                        }
                        Constants.ITERATE -> NativeFunction(0) {
                            val instance = it[0] as ListInstance
                            val count = instance.fields[Constants.COUNT] as Int
                            var i = 0
                            Instance(Vm.objectClass!!, false).apply {
                                fields[Constants.NEXT] = NativeFunction(0) {
                                    if (i == count) {
                                        null
                                    } else {
                                        val value = instance[i]
                                        i++
                                        value
                                    }
                                }
                            }
                        }
                        else -> null
                    }
                }
            },
            "Date" to object : NativeClass {
                override fun resolve(funcName: String): NativeFunction? {
                    return when (funcName) {
                        Constants.INIT -> NativeFunction(0) {
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