package net.globulus.simi.warp.native

import net.globulus.simi.Constants
import net.globulus.simi.warp.Instance
import net.globulus.simi.warp.ListInstance
import net.globulus.simi.warp.Vm
import net.globulus.simi.warp.mutabilityLockException

object Core : NativeModule {
    val keys = NativeFunction(0) {
        val instance = it[0] as Instance
        ListInstance(false, instance.fields.keys.toMutableList())
    }

    val listIterate = NativeFunction(0) {
        val instance = it[0] as ListInstance
        val count = instance.items.size
        var i = 0
        iterator {
            if (i == count) {
                null
            } else {
                val value = instance[i]
                i++
                value
            }
        }
    }

    override val classes: Map<String, NativeClass> = mapOf(
            Constants.CLASS_OBJECT to object : NativeClass {
                override fun resolve(funcName: String): NativeFunction? {
                    return when (funcName) {
                        "size" -> NativeFunction(0) {
                            val instance = it[0] as Instance
                            instance.fields.size.toLong()
                        }
                        "keys" -> keys
                        "values" -> NativeFunction(0) {
                            val instance = it[0] as Instance
                            ListInstance(false, instance.fields.values.toMutableList())
                        }
                        "zip" -> NativeFunction(0) {
                            val instance = it[0] as Instance
                            val zipped = instance.zipped()
                            Vm.newObject {
                                fields[Constants.ITERATE] = NativeFunction(0) {
                                    listIterate.func(listOf(zipped))
                                }
                            }
                        }
                        "zipped" -> NativeFunction(0) {
                            val instance = it[0] as Instance
                            instance.zipped()
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
                            if (instance.mutable) {
                                instance.fields.clear()
                                instance
                            } else {
                                mutabilityLockException()
                            }
                        }
                        "lock" -> NativeFunction(0) {
                            val instance = it[0] as Instance
                            instance.mutable = false
                            instance
                        }
                        "merge" -> NativeFunction(1) {
                            val instance = it[0] as Instance
                            if (instance.mutable) {
                                val from = it[1] as Instance
                                instance.fields.putAll(from.fields)
                                instance
                            } else {
                                mutabilityLockException()
                            }
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
                                is String -> getRawProp(instance, key)
                                is Instance -> {
                                    when (key.klass) {
                                        Vm.declaredClasses[Constants.CLASS_RANGE] -> instance.sublist(key.fields["from"] as Long, key.fields["to"] as Long)
                                        else -> null
                                    }
                                }
                                else -> null
                            }
                        }
                        Constants.SET -> NativeFunction(2) {
                            val instance = it[0] as ListInstance
                            if (instance.mutable) {
                                val key = it[1] as Long
                                val value = it[2] as Any
                                instance[key.toInt()] = value
                                null
                            } else {
                                mutabilityLockException()
                            }
                        }
                        "size" -> NativeFunction(0) {
                            val instance = it[0] as ListInstance
                            instance.items.size.toLong()
                        }
                        "add" -> NativeFunction(1) {
                            val instance = it[0] as ListInstance
                            if (instance.mutable) {
                                val value = it[1] as Any
                                instance += value
                                instance
                            } else {
                                mutabilityLockException()
                            }
                        }
                        "addAll" -> NativeFunction(1) {
                            val instance = it[0] as ListInstance
                            if (instance.mutable) {
                                val from = it[1] as ListInstance
                                instance.items.addAll(from.items)
                                instance
                            } else {
                                mutabilityLockException()
                            }
                        }
                        "isEmpty" -> NativeFunction(0) {
                            val instance = it[0] as ListInstance
                            instance.items.isEmpty()
                        }
                        "clear" -> NativeFunction(0) {
                            val instance = it[0] as ListInstance
                            if (instance.mutable) {
                                instance.items.clear()
                                instance
                            } else {
                                mutabilityLockException()
                            }
                        }
                        Constants.ITERATE -> listIterate
                        else -> null
                    }
                }
            },
            Constants.CLASS_STRING to object : NativeClass {
                override fun resolve(funcName: String): NativeFunction? {
                    return when (funcName) {
                        Constants.GET -> NativeFunction(1) {
                            val instance = it[0] as Instance
                            val string = instance.fields[Constants.PRIVATE] as String
                            when (val key = it[1]) {
                                is Long -> string[key.toInt()].toString()
                                is String -> getRawProp(instance, key)
                                is Instance -> {
                                    when (key.klass) {
//                                        Vm.rangeClass -> instance.sublist(key.fields["from"] as Long, key.fields["to"] as Long)
                                        else -> null
                                    }
                                }
                                else -> null
                            }
                        }
                        "replace" -> NativeFunction(3) {
                            val instance = it[0] as Instance
                            val string = instance.fields[Constants.PRIVATE] as String
                            val old = it[1] as String
                            val new = it[2] as String
                            val ignoreCase = it[3] as Boolean
                            string.replace(old, new, ignoreCase)
                        }
                        "builder" -> NativeFunction(0) {
                            Vm.newObject {
                                val builder = StringBuilder()
                                fields["add"] = NativeFunction(1) {
                                    builder.append(it[1])
                                    this
                                }
                                fields["build"] = NativeFunction(0) {
                                    builder.toString()
                                }
                            }
                        }
                        else -> null
                    }
                }
            }
    )

    private fun iterator(next: () -> Any?): Instance {
        return Vm.newObject {
            fields[Constants.NEXT] = NativeFunction(0) {
                next()
            }
        }
    }

    private fun Instance.zipped() = ListInstance(false,
            fields.map { (k, v) -> ListInstance(false, mutableListOf(k, v)) }.toMutableList()
    )

    private fun getRawProp(instance: Instance, name: String): Any? {
        Vm.instance?.getPropRaw(instance, name)
        return Vm.instance?.pop()
    }
}