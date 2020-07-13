### Native API

Šimi native API allows your Šimi code to inter-operate with code written in other languages. Currently, the only supported native API is JVM.

There are two steps to bridging native code into Šimi code: structuring the native code and importing it into Šimi code.

#### JVM native API
A native codebase living in a single JAR consists of one or more **NativeModules**. At its essence, a *NativeModule* just maps class names to their native implementation of type **NativeClass**. Similarly, a *NativeClass* maps method names to their **NativeFunction** implementations.

Let's look at the natively implemented **Date** module and its classes:
```kotlin
package net.globulus.simi.api // 1

import net.globulus.simi.Constants // 2
import net.globulus.simi.warp.Instance // 2
import net.globulus.simi.warp.SClass // 2
import net.globulus.simi.warp.native.NativeClass // 2
import net.globulus.simi.warp.native.NativeFunction // 2
import net.globulus.simi.warp.native.NativeModule // 2
import java.util.Date

class Date : NativeModule { // 3
    override val classes: Map<String, NativeClass> = mapOf( // 4
            "Date" to object : NativeClass { // 5
                override fun resolve(funcName: String): NativeFunction? { // 6
                    return when (funcName) {
                        Constants.INIT -> NativeFunction(0) { // 7
                            val instance = it[0] as Instance
                            val date = Date()
                            instance.apply {
                                fields["_"] = date
                                fields["time"] = date.time
                            }
                        }
                        "at" -> NativeFunction(1) { // 8
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
```

1. The package of the native module class should always be **net.globulus.simi.api**.
2. Use the exposed Šimi API classes to instantiate objects, get classes, interact with the VM, etc.
3. A JAR must contain a *NativeModule* implementation that is named as it - "Date.jar" must contain a class "Date" that implements *NativeModule*.
4. A *NativeModule* basically just statically maps class names to *NativeClass*es. This is because native class availability is resolved at compile-time.
5. Here is the native implementation of the Šimi class "Date".
6. Method resolution is dynamic and happens at runtime, hence the resolveFunc method. TODO improve
7. A **NativeFunction** is defined by its arity (number of arguments) and its body, which must return a value (it can be null, though). There's always an implicit, index zero argument corresponding to the method receiver. Here's an example of a **native init**. The first parameter

#### Native imports

Import native modules to Šimi code with the **import native** keywords:
```ruby
import native "Date"
```

You can specify the full path to a native module, just as you would for a Šimi file. The extension (".jar") can be omitted.