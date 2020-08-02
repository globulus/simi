package net.globulus.simi.warp

import net.globulus.simi.warp.native.NativeFunc

abstract class BoundCallable<T> {
    abstract  val receiver: Any
    abstract val callable: T

    override fun toString(): String {
        return callable.toString()
    }
}

class BoundMethod(override val receiver: Any,
                  override val callable: Closure
) : BoundCallable<Closure>()

class BoundNativeMethod(override val receiver: Any,
                        override val callable: NativeFunc
) : BoundCallable<NativeFunc>()

class BoundFiberTemplate(override val receiver: Any,
                         override val callable: FiberTemplate
) : BoundCallable<FiberTemplate>()

class BoundFiber(override val receiver: Any,
                 override val callable: Fiber
) : BoundCallable<Fiber>()