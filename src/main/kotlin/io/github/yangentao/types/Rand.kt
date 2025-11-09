@file:Suppress("MemberVisibilityCanBePrivate")

package io.github.yangentao.types

import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.random.nextLong

object Rand {
    val random = Random(System.currentTimeMillis())
    private const val MIN_ID: Long = 0x10000000
    private const val MAX_ID: Long = 0x7FFFFFFF

    fun nextID(): Long {
        return random.nextLong(MIN_ID, MAX_ID)
    }

    fun nextDouble(from: Double, until: Double): Double {
        return random.nextDouble(from, until)
    }

    fun nextInt(range: IntRange): Int {
        return random.nextInt(range)
    }

    fun nextLong(range: LongRange): Long {
        return random.nextLong(range)
    }

    fun nextInt(from: Int, until: Int): Int {
        return random.nextInt(from, until)
    }

    fun nextLong(from: Long, until: Long): Long {
        return random.nextLong(from, until)
    }

    val code4: String get() = nextInt(1000, 9999).toString()
    val code6: String get() = nextInt(100_000, 999_999).toString()
    val phone: String get() = nextLong(130_0000_0000, 198_0000_0000).toString()
}