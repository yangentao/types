package io.github.yangentao.types

import kotlin.math.max
import kotlin.reflect.KClass



@Suppress("UNCHECKED_CAST")
class EList<T : Any>(private var buffer: Array<T?>, size: Int = buffer.size) : AbstractMutableList<T>() {

    val elementType: KClass<T> get() = buffer::class.java.componentType.kotlin as KClass<T>
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

    fun getOr(index: Int): T? {
        if (index in indices) return buffer[index] as T
        return null
    }

    override operator fun get(index: Int): T {
        if (index in indices) return buffer[index] as T
        throw IndexOutOfBoundsException(index)
    }

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
        val old = buffer[index]
        if (index < size - 1) {
            System.arraycopy(buffer, index + 1, buffer, index, size - 1 - index)
        }
        size -= 1
        return old as T
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
        val s = "EList<${elementType.simpleName}>($size)[${this.joinToString(", ")}]"
        println(s)
    }

    companion object {
        inline operator fun <reified T : Any> invoke(capacity: Int = 10): EList<T> {
            return EList(Array(capacity) { null }, 0)
        }

        inline operator fun <reified T : Any> invoke(collection: Collection<T>): EList<T> {
            return EList(collection.toTypedArray())
        }

        operator fun <T : Any> invoke(type: KClass<T>, capacity: Int = 10): EList<T> {
            val arr = RefArray.newInstance(type.java, capacity)
            return EList(arr as Array<T?>, 0)
        }

        inline fun <reified T : Any> empty(): EList<T> {
            val arr = RefArray.newInstance(T::class.java, 0)
            return EList(arr as Array<T?>, 0)
        }

        fun <T : Any> empty(type: KClass<T>): EList<T> {
            val arr = RefArray.newInstance(type.java, 0)
            return EList(arr as Array<T?>, 0)
        }

        inline fun <reified T : Any> of(vararg elements: T): EList<T> {
            return EList(arrayOf(*elements), elements.size)
        }
    }
}




//
//open class A(val value: Int) {
//    override fun toString(): String {
//        return "A($value)"
//    }
//}
//
//class B(value: Int) : A(value) {
//    override fun toString(): String {
//        return "B($value)"
//    }
//}
//
//fun main() {
////    val ja = EList(listOf(1, 2, 3))
//    val ja = EList(arrayOf(1, 2, 3))
//    ja.dump()
//
//}
