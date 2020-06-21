package net.globulus.simi.warp

import net.globulus.simi.Constants

fun mutabilityLockException() = Instance(Vm.mutabilityLockExceptionClass!!, false)
fun illegalArgumentException(message: String) = Instance(Vm.illegalArgumentException!!, false).apply {
    fields[Constants.MESSAGE] = message
}