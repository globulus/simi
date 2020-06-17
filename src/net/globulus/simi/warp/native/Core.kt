package net.globulus.simi.warp.native

import net.globulus.simi.Constants
import net.globulus.simi.warp.Instance
import net.globulus.simi.warp.ListInstance
import net.globulus.simi.warp.Vm

object Core : NativeModule {
    val keys = NativeFunction(0) {
        val instance = it[0] as Instance
        ListInstance(false, instance.fields.keys.toMutableList())
    }

    val listIterate = NativeFunction(0) {
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
    override val classes: Map<String, NativeClass> = mapOf(
            Constants.CLASS_OBJECT to object : NativeClass {
                override fun resolve(funcName: String): NativeFunction? {
                    return when (funcName) {
                        "keys" -> keys
                        "values" -> NativeFunction(0) {
                            val instance = it[0] as Instance
                            ListInstance(false, instance.fields.values.toMutableList())
                        }
                        "zip" -> NativeFunction(0) {
                            val instance = it[0] as Instance
                            ListInstance(false, instance.fields.map { (k, v) -> ListInstance(false, mutableListOf(k, v)) }.toMutableList())
                        }
                        "isEmpty" -> NativeFunction(0) {
                            val instance = it[0] as Instance
                            instance.fields.isEmpty()
                        }
                        Constants.ITERATE -> NativeFunction(0) {
                            listIterate.func(listOf(keys.func(it)))
                        }
                        "clear" -> NativeFunction(0) {
                            val instance = it[0] as Instance
                            instance.fields.clear()
                            instance
                        }
                        "merge" -> NativeFunction(1) {
                            val instance = it[0] as Instance
                            val from = it[1] as Instance
                            instance.fields.putAll(from.fields)
                            instance
                         }
                        else -> null
                    }
                }
            },
            Constants.CLASS_LIST to object : NativeClass {
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
                                is Instance -> {
                                    when (key.klass) {
                                        Vm.rangeClass -> {

                                        }
                                        else -> null
                                    }
                                }
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
                            instance
                        }
                        "addAll" -> NativeFunction(1) {
                            val instance = it[0] as ListInstance
                            val from = it[1] as ListInstance
                            instance.items.addAll(from.items)
                            instance
                        }
                        "isEmpty" -> NativeFunction(0) {
                            val instance = it[0] as ListInstance
                            instance.items.isEmpty()
                        }
                        "clear" -> NativeFunction(0) {
                            val instance = it[0] as ListInstance
                            instance.items.clear()
                            instance
                        }
                        Constants.ITERATE -> listIterate
                        else -> null
                    }
                }
            }
    )
}