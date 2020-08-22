package net.globulus.simi.warp

import java.lang.ref.WeakReference

class Upvalue(var location: WeakReference<Any>,
              val fiberName: String,
              var closed: Boolean,
              var next: Upvalue?
) {
    val sp: Int get() {
        val loc = location.get()!!
        if (loc is Int) {
            return loc
        } else {
            throw IllegalStateException("Trying to read Upvalue sp when it's already closed")
        }
    }
}