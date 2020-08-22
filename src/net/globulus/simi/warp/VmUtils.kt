package net.globulus.simi.warp

import net.globulus.simi.Constants

fun mutabilityLockException() = Instance(Vm.declaredClasses[Constants.EXCEPTION_MUTABILITY_LOCK]!!, false)
fun illegalArgumentException(message: String) = Instance(Vm.declaredClasses[Constants.EXCEPTION_ILLEGAL_ARGUMENT]!!, false).apply {
    fields[Constants.MESSAGE] = message
}