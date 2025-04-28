@file:Suppress("unused")

package io.github.yangentao.types


import java.nio.ByteOrder

//newSize =  size * 8
fun ByteArray.toBitList(): List<Int> {
    if (this.isEmpty()) return emptyList()
    val nList = ArrayList<Int>(this.size * 8)
    this.forEach { b ->
        val nVal = b.toInt()
        for (i in 0..7) {
            val n = if (i == 0) (nVal and 0x01) else ((nVal ushr i) and 0x01)
            nList.add(n)
        }
    }
    return nList
}

fun ByteArray.subArray(from: Int, size: Int): ByteArray {
    if (this.isEmpty()) return ByteArray(0)
    if (from >= this.size) return ByteArray(0)
    if (from + size >= this.size) return this.sliceArray(from..<this.size)
    return this.sliceArray(from..<(from + size))
}

val Byte.uintValue: Int get() = this.toInt() and 0xFF

val Int.high0: Byte get() = ((this ushr 24) and 0x00ff).toByte()
val Int.high1: Byte get() = ((this ushr 16) and 0x00ff).toByte()
val Int.high2: Byte get() = ((this ushr 8) and 0x00ff).toByte()
val Int.high3: Byte get() = (this and 0x00ff).toByte()

val Int.low0: Byte get() = (this and 0x00ff).toByte()
val Int.low1: Byte get() = ((this ushr 8) and 0x00ff).toByte()
val Int.low2: Byte get() = ((this ushr 16) and 0x00ff).toByte()
val Int.low3: Byte get() = ((this ushr 24) and 0x00ff).toByte()

val ByteArray.strUTF8: String get() = String(this, Charsets.UTF_8)

fun Int.toByteArray(order: ByteOrder = ByteOrder.BIG_ENDIAN): ByteArray {
    return if (order == ByteOrder.BIG_ENDIAN) {
        byteArrayOf(low3, low2, low1, low0)
    } else {
        byteArrayOf(low0, low1, low2, low3)
    }
}

fun Float.toByteArray(order: ByteOrder): ByteArray {
    val buf = ByteArray(4)
    val n: Int = java.lang.Float.floatToIntBits(this)
    if (order == ByteOrder.LITTLE_ENDIAN) {
        buf[0] = n.low0
        buf[1] = n.low1
        buf[2] = n.low2
        buf[3] = n.low3
    } else {
        buf[0] = n.high0
        buf[1] = n.high1
        buf[2] = n.high2
        buf[3] = n.high3
    }
    return buf
}

//TODO delete it, use Float.toByteArray instead.
//50 => 00 00 48 42
fun float2bytes(v: Float): ByteArray {
    val buf = ByteArray(4)
    val n: Int = java.lang.Float.floatToIntBits(v)
    buf[0] = n.low0
    buf[1] = (n ushr 8).low0
    buf[2] = (n ushr 16).low0
    buf[3] = (n ushr 24).low0
    return buf
}

//bytes2Float(0, 0, 0x48, 0x42) => 50.0
fun bytes2Float(b0: Int, b1: Int, b2: Int, b3: Int): Float {
    val n: Int = (b0 and 0xFF) or ((b1 shl 8) and 0x00FF00) or ((b2 shl 16) and 0x00FF0000) or (b3 shl 24)
    return java.lang.Float.intBitsToFloat(n)
}

fun bytes2Int(low0: Int, low1: Int, low2: Int, low3: Int): Int {
    val n: Int = (low0 and 0xFF) or ((low1 shl 8) and 0x00FF00) or ((low2 shl 16) and 0x00FF0000) or (low3 shl 24)
    return n
}

data class IntBytes(var value: Int) {
    var low0: Byte
        get() = (value and 0x00ff).toByte()
        set(v) {
            value = (value and 0xffffff00.toInt()) or (v.toInt() and 0xff)
        }
    val low1: Byte get() = ((value ushr 8) and 0x00ff).toByte()
    val low2: Byte get() = ((value ushr 16) and 0x00ff).toByte()
    val low3: Byte get() = ((value ushr 24) and 0x00ff).toByte()

    val high0: Byte get() = ((value ushr 24) and 0x00ff).toByte()
    val high1: Byte get() = ((value ushr 16) and 0x00ff).toByte()
    val high2: Byte get() = ((value ushr 8) and 0x00ff).toByte()
    val high3: Byte get() = (value and 0x00ff).toByte()
}

object ByteConst {
    const val NL: Byte = 0
    const val LK: Byte = 123 // {
    const val RK: Byte = 125 // }
    const val QT: Byte = 34  // "
    const val ES: Byte = 92  // \
    const val SP: Byte = 32  // 空格
    const val CR: Byte = 13  // CR
    const val LF: Byte = 10  // LF
    const val TB: Byte = 9   // TAB
}