package net.globulus.simi.api

import net.globulus.simi.Constants
import net.globulus.simi.warp.Instance
import net.globulus.simi.warp.ListInstance
import net.globulus.simi.warp.Vm
import net.globulus.simi.warp.native.NativeClass
import net.globulus.simi.warp.native.NativeFunction
import net.globulus.simi.warp.native.NativeModule
import net.globulus.simi.warp.toSimiList
import java.io.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class File : NativeModule {
    override val classes: Map<String, NativeClass> = mapOf(
            "File" to object : NativeClass {
                override fun resolve(funcName: String): NativeFunction? {
                    return when (funcName) {
                        "isDirectory" -> NativeFunction(0) {
                            Files.isDirectory(Paths.get(getPath(it, 0)))
                        }
                        "list" -> NativeFunction(0) {
                            File(getPath(it, 0)).list().toSimiList()
                        }
                        "readLines" -> NativeFunction(0) {
                            try {
                                val lines = Files.readAllLines(Paths.get(getPath(it, 0)))
                                ListInstance(true, lines.toMutableList())
                            } catch (e: IOException) {
                                raiseIoException(e)
                            }
                        }
                        else -> null
                    }
                }
            },
            "ReadStream" to object : NativeClass {
                override fun resolve(funcName: String): NativeFunction? {
                    return when (funcName) {
                        Constants.INIT -> NativeFunction(1) {
                            val instance = it[0] as Instance
                            val path = getPath(it, 1)
                            try {
                                val reader = BufferedReader(FileReader(path))
                                instance.apply {
                                    fields[Constants.PRIVATE] = reader
                                    fields["file"] = it[1]!!
                                }
                            } catch (e: FileNotFoundException) {
                                raiseIoException(e)
                            }
                        }
                        "read" -> NativeFunction(0) {
                            try {
                                getReader(it).read().toChar().toString()
                            } catch (e: IOException) {
                                raiseIoException(e)
                            }
                        }
                        "readLine" -> NativeFunction(0) {
                            try {
                                getReader(it).readLine()
                            } catch (e: IOException) {
                                raiseIoException(e)
                            }
                        }
                        "reset" -> NativeFunction(0) {
                            try {
                                getReader(it).reset()
                                it[0]
                            } catch (e: IOException) {
                                raiseIoException(e)
                            }
                        }
                        "skip" -> NativeFunction(1) {
                            try {
                                getReader(it).skip(it[1] as Long)
                            } catch (e: IOException) {
                                raiseIoException(e)
                            }
                        }
                        "close" -> NativeFunction(0) {
                            try {
                                getReader(it).close()
                                null
                            } catch (e: IOException) {
                                raiseIoException(e)
                            }
                        }
                        else -> null
                    }
                }

                fun getReader(args: List<Any?>) = (args[0] as Instance)[Constants.PRIVATE] as BufferedReader
            },
            "WriteStream" to object : NativeClass {
                override fun resolve(funcName: String): NativeFunction? {
                    return when (funcName) {
                        Constants.INIT -> NativeFunction(1) {
                            val instance = it[0] as Instance
                            val path = getPath(it, 1)
                            try {
                                val file = File(path)
                                file.parentFile?.let { parentFile ->
                                    parentFile.mkdirs()
                                }
                                file.createNewFile()
                                val writer = BufferedWriter(FileWriter(path))
                                instance.apply {
                                    fields[Constants.PRIVATE] = writer
                                    fields["file"] = it[1]!!
                                }
                            } catch (e: FileNotFoundException) {
                                raiseIoException(e)
                            }
                        }
                        "write" -> NativeFunction(1) {
                            try {
                                getWriter(it).write(it[1] as String)
                                it[0]
                            } catch (e: IOException) {
                                raiseIoException(e)
                            }
                        }
                        "close" -> NativeFunction(0) {
                            try {
                                getWriter(it).close()
                                null
                            } catch (e: IOException) {
                                raiseIoException(e)
                            }
                        }
                        else -> null
                    }
                }

                fun getWriter(args: List<Any?>) = (args[0] as Instance)[Constants.PRIVATE] as BufferedWriter
            }
    )

    private fun getPath(args: List<Any?>, index: Int) = (args[index] as Instance)["path"] as String

    private fun raiseIoException(e: Exception): Instance {
        return Vm.newInstance("IoException") {
            fields[Constants.MESSAGE] = e.localizedMessage
        }
    }
}