package net.globulus.simi.warp

class Fiber(val closure: Closure) {
    internal var state = State.NEW

    internal var sp = 0
    internal var stackSize = Vm.INITIAL_STACK_SIZE
    internal var stack = arrayOfNulls<Any>(Vm.INITIAL_STACK_SIZE)

    internal var fp = 0
        set(value) {
            field = value
            if (value > 0) {
                frame = callFrames[value - 1]!!
            }
        }
    internal lateinit var frame: CallFrame
    internal val callFrames = arrayOfNulls<CallFrame>(Vm.MAX_FRAMES)

    internal lateinit var caller: Fiber

    internal enum class State {
        NEW, STARTED
    }
}