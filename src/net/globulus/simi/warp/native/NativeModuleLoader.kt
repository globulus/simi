package net.globulus.simi.warp.native

import net.globulus.simi.api.Constants
import java.net.URL
import java.net.URLClassLoader

object NativeModuleLoader {
    private val classes = mutableMapOf<String, NativeClass>()

    fun load(path: String, moduleName: String, useCustomLoader: Boolean = false) {
        try {
            val apiClassName = getApiClassName(moduleName)
            val module = if (useCustomLoader) {
                val url = URL(path)
                val loader: ClassLoader = URLClassLoader.newInstance(arrayOf(url), javaClass.classLoader)
                Class.forName(apiClassName, true, loader).newInstance()
            } else {
                Class.forName(apiClassName).newInstance()
            } as NativeModule
            classes.putAll(module.classes)
        } catch (e: Exception) {
            e.printStackTrace()
            if (!useCustomLoader) {
                load(path, moduleName, true) // Retry
            }
        }
    }

    fun resolve(className: String, funcName: String): NativeFunction? {
        return classes[className]?.resolve(funcName)
    }

    private fun getApiClassName(moduleName: String) = Constants.PACKAGE_SIMI_API + '.' + moduleName
}