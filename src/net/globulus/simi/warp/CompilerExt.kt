package net.globulus.simi.warp

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

fun MutableList<Byte>.put(byte: Byte): Int {
    add(byte)
    return 1
}

internal fun MutableList<Byte>.put(opCode: OpCode): Int {
    add(opCode.byte)
    return 1
}

fun MutableList<Byte>.put(byteArray: ByteArray): Int {
    for (b in byteArray) {
        add(b)
    }
    return byteArray.size
}

fun Int.toByteArray(): ByteArray {
    return ByteBuffer.allocate(Int.SIZE_BYTES).putInt(this).array()
}

fun MutableList<Byte>.putInt(i: Int): Int {
    val ba = i.toByteArray()
    put(ba)
    return ba.size
}

fun MutableList<Byte>.setInt(i: Int, pos: Int) {
    for ((idx, b) in i.toByteArray().withIndex()) {
        set(pos + idx, b)
    }
}

fun MutableList<Byte>.emitMarkingPosition(block: MutableList<Byte>.() -> Unit): Int {
    val skipPos = size
    putInt(0)
    block()
    setInt(size, skipPos)
    return size
}

fun MutableList<Byte>.putLong(l: Long): Int {
    val ba = ByteBuffer.allocate(Long.SIZE_BYTES).putLong(l).array()
    put(ba)
    return ba.size
}

fun MutableList<Byte>.putDouble(d: Double): Int {
    val ba = ByteBuffer.allocate(8).putDouble(d).array()
    put(ba)
    return ba.size
}

internal fun ByteArrayOutputStream.put(opCode: OpCode) {
    write(opCode.byte.toInt())
}

fun ByteArrayOutputStream.putInt(i: Int) {
    write(ByteBuffer.allocate(Int.SIZE_BYTES).putInt(i).array())
}

fun ByteArrayOutputStream.putLong(l: Long) {
    write(ByteBuffer.allocate(Long.SIZE_BYTES).putLong(l).array())
}

fun ByteArrayOutputStream.putDouble(d: Double) {
    write(ByteBuffer.allocate(8).putDouble(d).array())
}

fun ByteBuffer.readMarkedPosition(block: () -> Unit) {
    val originPos = int
    while (position() < originPos) {
        block()
    }
}

fun String.lastNameComponent() = split('.').last()

fun Map<String, Any>.toSimiObject(): Instance {
    val instance = Instance(Vm.objectClass!!, false)
    for ((k, v) in entries) {
        instance.fields[k] = when (v) {
            is Array<*> -> v.toSimiList()
            else -> v
        }
    }
    return instance
}

fun Array<*>.toSimiList(): ListInstance {
    return ListInstance(false, (this as? Array<Any>)?.toMutableList())
}
