package io.github.yangentao.types

import java.text.DecimalFormat

import kotlin.math.ceil

const val KB: Int = 1024
const val MB: Int = 1024 * 1024
const val GB: Int = 1024 * 1024 * 1024

val Int.MB: Int get() = this * 1024 * 1024
val Long.MB: Long get() = this * 1024 * 1024

val Double.ceilInt: Int get() = ceil(this).toInt()

fun <T : Comparable<T>> minValue(a: T, vararg vs: T): T {
    val x = vs.minOrNull() ?: return a
    if (a <= x) return a
    return x
}

fun <T : Comparable<T>> maxValue(a: T, vararg vs: T): T {
    val x = vs.maxOrNull() ?: return a
    if (a >= x) return a
    return x
}

fun Number.format(integers: Int, fractions: Int): String {
    val df = DecimalFormat()
    df.isGroupingUsed = false
    if (integers > 0) df.minimumIntegerDigits = integers
    if (fractions > 0) {
        df.minimumFractionDigits = fractions
        df.maximumFractionDigits = fractions
    }
    return df.format(this)
}

fun Number.fractionSize(size: Int): String {
    val df = DecimalFormat()
    df.maximumFractionDigits = size
    df.minimumFractionDigits = size
    return df.format(this)
}

fun Number.maxFraction(size: Int): String {
    val df = DecimalFormat()
    df.maximumFractionDigits = size
    return df.format(this)
}

//12345.format(",###.##")
//12345.6789.format("0,000.00")
//@see DecimalFormat
fun Number.format(pattern: String): String {
    return if (pattern.isEmpty()) {
        this.toString()
    } else {
        DecimalFormat(pattern).format(this)
    }
}

data class BitValue(var value: Int) {

    fun has(bit: Int): Boolean {
        return value and bit != 0
    }

    fun add(bit: Int): BitValue {
        value = value or bit
        return this
    }

    fun remove(bit: Int): BitValue {
        value = value and bit.inv()
        return this
    }
}

fun Int.hasAnyBit(bit: Int): Boolean {
    return this and bit != 0
}

fun Int.hasAllBit(bit: Int): Boolean {
    return this and bit == bit
}