package net.globulus.simi.warp.native

import net.globulus.simi.Constants
import net.globulus.simi.warp.*

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
                        "sorted" -> NativeFunction(0) {
                            val instance = it[0] as ListInstance
                            ListInstance(true, instance.items.sortedWith(Comparator { o1, o2 ->
                                if (o1 is String && o2 is String) {
                                    o1.compareTo(o2)
                                } else {
                                    throw IllegalArgumentException("Sort not fully implemented.")
                                }
                            }).toMutableList())
                        }
                        Constants.HAS -> NativeFunction(1) {
                            val instance = it[0] as ListInstance
                            it[1] in instance.items
                        }
                        "distinct" -> NativeFunction(0) {
                            val instance = it[0] as ListInstance
                            ListInstance(true, instance.items.distinct().toMutableList())
                        }
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
                                        Vm.declaredClasses[Constants.CLASS_RANGE] ->
                                            string.substring((key.fields["from"] as Long).toInt(), (key.fields["to"] as Long).toInt())
                                        else -> null
                                    }
                                }
                                else -> null
                            }
                        }
                        Constants.HAS -> NativeFunction(1) {
                            val string = stringValue(it)
                            val other = it[1] as String
                            other in string
                        }
                        "replacing" -> NativeFunction(3) {
                            val string = stringValue(it)
                            val old = it[1] as String
                            val new = it[2] as String
                            val ignoreCase = it[3] as Boolean
                            string.replace(old, new, ignoreCase)
                        }
                        "replacingRegex" -> NativeFunction(2) {
                            val string = stringValue(it)
                            val old = it[1] as String
                            val new = it[2] as String
                            string.replace(Regex(old), new)
                        }
                        "startsWith" -> NativeFunction(2) {
                            val string = stringValue(it)
                            val prefix = it[1] as String
                            val ignoreCase = it[2] as Boolean
                            string.startsWith(prefix, ignoreCase)
                        }
                        "endsWith" -> NativeFunction(2) {
                            val string = stringValue(it)
                            val suffix = it[1] as String
                            val ignoreCase = it[2] as Boolean
                            string.endsWith(suffix, ignoreCase)
                        }
                        "findAllRegex" -> NativeFunction(1) {
                            val string = stringValue(it)
                            val regex = Regex(it[1] as String)
                            ListInstance(true, regex.findAll(string).map { result ->
                                Vm.newObject {
                                    fields["value"] = result.value
                                    fields["range"] = Vm.newInstance("Range") {
                                        fields["from"] = result.range.first.toLong()
                                        fields["to"] = result.range.last.toLong()
                                    }
                                }
                            }.toMutableList())
                        }
                        "matchesRegex" -> NativeFunction(1) {
                            val string = stringValue(it)
                            val regex = Regex(it[1] as String)
                            regex.matches(string)
                        }
                        "indexOf" -> NativeFunction(3) {
                            val string = stringValue(it)
                            val other = it[1] as String
                            val startIndex = it[2] as Long
                            val ignoreCase = it[3] as Boolean
                            string.indexOf(other, startIndex.toInt(), ignoreCase).toLong()
                        }
                        "upperCased" -> NativeFunction(0) {
                            val string = stringValue(it)
                            string.toUpperCase()
                        }
                        "lowerCased" -> NativeFunction(0) {
                            val string = stringValue(it)
                            string.toLowerCase()
                        }
                        "split" -> NativeFunction(1) {
                            val string = stringValue(it)
                            val delimiter = it[1] as String
                            string.split(delimiter).toSimiList()
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

                fun stringValue(args: List<Any?>) = (args[0] as Instance).fields[Constants.PRIVATE] as String
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