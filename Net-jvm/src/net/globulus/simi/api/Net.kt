package net.globulus.simi.api

import net.globulus.simi.Constants
import net.globulus.simi.warp.Instance
import net.globulus.simi.warp.Vm
import net.globulus.simi.warp.native.AsyncNativeFunction
import net.globulus.simi.warp.native.NativeClass
import net.globulus.simi.warp.native.NativeFunc
import net.globulus.simi.warp.native.NativeModule
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils

class Net : NativeModule {
    override val classes: Map<String, NativeClass> = mapOf(
            "Net" to object : NativeClass {
                override fun resolve(funcName: String): NativeFunc? {
                    return when (funcName) {
                        "httpRequest" -> AsyncNativeFunction(1) { args: List<Any?>, callback: (Any?) -> Unit ->
                            val request = args[1] as Instance
                            Thread {
                                val client = HttpClients.createDefault()
                                val url = request["url"] as String
                                val baseRequest = when (request["verb"] as String) {
                                    "GET" -> HttpGet(url)
                                    else -> throw IllegalArgumentException("WTF")
                                }
                                (request["headers"] as? Instance)?.let {
                                    for ((k, v) in it.fields) {
                                        baseRequest.setHeader(k, v as String)
                                    }
                                }
                                try {
                                    val response = client.execute(baseRequest)
                                    client.close()
                                    callback(Vm.newObject {
                                        fields["code"] = response.statusLine.statusCode
                                        fields["body"] = EntityUtils.toString(response.entity)
                                    })
                                } catch (e: Exception) {
                                    callback(Vm.newInstance("Exception") {
                                        fields[Constants.MESSAGE] = e.localizedMessage
                                    })
                                }
                            }.start()
                        }
                        else -> null
                    }
                }

            },
            "Net.Json" to object : NativeClass {
                override fun resolve(funcName: String): NativeFunc? {
                    TODO("Not yet implemented")
                }
            }
    )
}