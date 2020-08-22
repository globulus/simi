package net.globulus.simi.api

import net.globulus.simi.Constants
import net.globulus.simi.warp.Instance
import net.globulus.simi.warp.SClass
import net.globulus.simi.warp.native.NativeClass
import net.globulus.simi.warp.native.NativeFunction
import net.globulus.simi.warp.native.NativeModule
import java.util.Date

class Date : NativeModule {
    override val classes: Map<String, NativeClass> = mapOf(
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