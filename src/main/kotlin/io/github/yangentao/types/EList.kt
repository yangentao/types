package io.github.yangentao.types

import kotlin.math.max
import kotlin.reflect.KClass

// 带类型的List
@Suppress("UNCHECKED_CAST")
class EList<T : Any>(private var buffer: JavaArray, size: Int = buffer.length) : AbstractMutableList<T>() {

    constructor(array: Array<T>) : this(JavaArray(array), array.size)

    val elementClass: KClass<T> get() = buffer.elementClass as KClass<T>
    val capacity: Int get() = buffer.length
    override var size: Int = size
        private set
    val indices: IntRange get() = 0..<size

    private fun grow(minGrow: Int = 8) {
        val n: Int = if (buffer.length < 512) {
            buffer.length + max(buffer.length, minGrow)
        } else {
            buffer.length + max(buffer.length shr 1, minGrow)
        }
        buffer.newLength(max(10, n))
    }

    fun ensureCapacity(c: Int) {
        if (capacity < c) {
            buffer.newLength(c)
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
            System.arraycopy(buffer.array, index + 1, buffer.array, index, size - 1 - index)
        }
        size -= 1
        return old as T
    }

    override fun add(index: Int, element: T) {
        if (index !in 0..size) throw IndexOutOfBoundsException(index)
        if (size >= capacity) grow()
        if (index < size) {
            System.arraycopy(buffer.array, index, buffer.array, index + 1, size - index)
        }
        buffer[index] = element
        size += 1
    }

    fun dump() {
        val s = "EList<${elementClass.simpleName}>($size)[${this.joinToString(", ")}]"
        println(s)
    }

    companion object {

        operator fun <T : Any> invoke(type: KClass<T>, capacity: Int = 10): EList<T> {
            return EList(JavaArray.create(type, capacity), 0)
        }

        inline operator fun <reified T : Any> invoke(capacity: Int = 10): EList<T> {
            return EList(JavaArray.create(T::class, capacity), 0)
        }

        fun <T : Any> empty(type: KClass<T>): EList<T> {
            return EList(JavaArray.create(type, 0), 0)
        }

        inline fun <reified T : Any> empty(): EList<T> {
            return EList(JavaArray.create(T::class, 0), 0)
        }

        inline operator fun <reified T : Any> invoke(collection: Collection<T>): EList<T> {
            return EList(JavaArray(collection.toTypedArray()), collection.size)
        }

        operator fun <T : Any> invoke(array: CharArray): EList<Char> {
            return EList(JavaArray(array), array.size)
        }

        operator fun <T : Any> invoke(array: ByteArray): EList<Byte> {
            return EList(JavaArray(array), array.size)
        }

        operator fun <T : Any> invoke(array: ShortArray): EList<Short> {
            return EList(JavaArray(array), array.size)
        }

        operator fun <T : Any> invoke(array: IntArray): EList<Int> {
            return EList(JavaArray(array), array.size)
        }

        operator fun <T : Any> invoke(array: LongArray): EList<Long> {
            return EList(JavaArray(array), array.size)
        }

        operator fun <T : Any> invoke(array: FloatArray): EList<Float> {
            return EList(JavaArray(array), array.size)
        }

        operator fun <T : Any> invoke(array: DoubleArray): EList<Double> {
            return EList(JavaArray(array), array.size)
        }

        inline fun <reified T : Any> of(vararg elements: T): EList<T> {
            return EList(JavaArray(elements), elements.size)
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
