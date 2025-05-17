package io.github.yangentao.types

import kotlin.math.max
import kotlin.reflect.KClass

private typealias RArray = java.lang.reflect.Array

class EList<T>(private var buffer: Array<T?>, size: Int = buffer.size) : AbstractMutableList<T>() {

    val elementType: KClass<*> get() = buffer::class.java.componentType.kotlin
    val capacity: Int get() = buffer.size
    override var size: Int = size
        private set
    val indices: IntRange get() = 0..<size

    private fun grow(minGrow: Int = 8) {
        val n: Int = if (buffer.size < 512) {
            buffer.size + max(buffer.size, minGrow)
        } else {
            buffer.size + max(buffer.size shr 1, minGrow)
        }
        buffer = buffer.copyOf(max(10, n))
    }

    fun ensureCapacity(c: Int) {
        if (capacity < c) {
            buffer = buffer.copyOf(c)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun getOr(index: Int): T? {
        if (index in indices) return buffer[index] as T
        return null
    }

    @Suppress("UNCHECKED_CAST")
    override operator fun get(index: Int): T {
        if (index in indices) return buffer[index] as T
        throw IndexOutOfBoundsException(index)
    }

    @Suppress("UNCHECKED_CAST")
    override operator fun set(index: Int, element: T): T {
        when (index) {
            size -> {
                add(element)
                return element
            }

            in indices -> {
                val old = buffer[index]
                buffer[index] = element
                return old as T
            }

            else -> throw IndexOutOfBoundsException(index)
        }
    }

    override fun removeAt(index: Int): T {
        if (index !in indices) throw IndexOutOfBoundsException(index)
        val old = get(index)
        if (index < size - 1) {
            System.arraycopy(buffer, index + 1, buffer, index, size - 1 - index)
        }
        size -= 1
        return old
    }

    override fun add(index: Int, element: T) {
        if (index !in 0..size) throw IndexOutOfBoundsException(index)
        if (size >= capacity) grow()
        if (index < size) {
            System.arraycopy(buffer, index, buffer, index + 1, size - index)
        }
        buffer[index] = element
        size += 1
    }

    fun dump() {
        val s = "EList<${elementType.simpleName}>($size)[${this.joinToString(", ") { it.toString() }}]"
        println(s)
    }

    companion object {
        inline operator fun <reified T> invoke(capacity: Int = 10): EList<T> {
            return EList(Array(capacity) { null }, 0)
        }

        inline fun <reified T> of(vararg elements: T): EList<T> {
            return EList(arrayOf(*elements), elements.size)
        }
    }
}

//fun main() {
//    val ja = EList<Int>()
//    ja[0] = 10
////    ja.removeAt(2)
//    ja.add(99)
//    ja.dump()
//
//}
