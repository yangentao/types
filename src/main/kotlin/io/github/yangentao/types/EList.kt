package io.github.yangentao.types

import kotlin.math.max
import kotlin.reflect.KClass

private typealias RArray = java.lang.reflect.Array

@Suppress("UNCHECKED_CAST")
class EList<T>(private var buffer: Array<T?>) : Iterable<T> {
    val elementType: KClass<*> get() = buffer::class.java.componentType.kotlin
    val capacity: Int get() = buffer.size
    var size: Int = 0
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

    fun addAll(ls: Collection<T>) {
        ensureCapacity(capacity + ls.size)
        for (v in ls) add(v)
    }

    fun add(value: T) {
        if (size >= capacity) {
            grow()
        }
        buffer[size] = value
        size += 1
    }

    fun getOr(index: Int): T? {
        if (index in indices) return buffer[index] as T
        return null
    }

    operator fun get(index: Int): T {
        if (index in indices) return buffer[index] as T
        throw IndexOutOfBoundsException(index)
    }

    operator fun set(index: Int, value: T) {
        when (index) {
            size -> add(value)
            in indices -> buffer[index] = value
            else -> throw IndexOutOfBoundsException(index)
        }
    }

    override operator fun iterator(): Iterator<T> {
        return JArrayIterator()
    }

    inner class JArrayIterator() : Iterator<T> {
        private var index: Int = 0

        override fun next(): T {
            return get(index++)
        }

        override fun hasNext(): Boolean {
            return index < size
        }

    }

    fun dump() {
        val s = "JArray<${elementType.simpleName}>($size)[${this.joinToString(", ") { it.toString() }}]"
        println(s)
    }

    companion object {
        inline operator fun <reified T> invoke(capacity: Int = 10): EList<T> {
            return EList<T>(Array<T?>(capacity) { null })
        }

    }
}

fun main() {
    val ja = EList<Int>()
    ja[0] = 10
    ja[1] = 20
    ja.addAll(listOf(2, 3, 4, 5))
    ja.dump()
    for (v in ja) {
        println(v)
    }

}
