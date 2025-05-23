package io.github.yangentao.types

import kotlin.math.min
import kotlin.reflect.KClass

typealias RefArray = java.lang.reflect.Array

class JavaArray(var array: Any) : Iterable<Any?> {
    private val elementJClass: Class<*> get() = this.array::class.java.componentType
    val elementClass: KClass<*> get() = this.array::class.java.componentType.kotlin
    val length: Int get() = RefArray.getLength(array)
    val isEmpty: Boolean get() = length == 0
    val isNotEmpty: Boolean get() = length > 0

    init {
        assert(array.javaClass.isArray)
    }

    fun newLength(newLen: Int) {
        if (newLen == length) return
        val newArray = RefArray.newInstance(elementJClass, newLen)
        System.arraycopy(array, 0, newArray, 0, min(length, newLen))
        array = newArray
    }

    operator fun get(index: Int): Any? {
        return RefArray.get(array, index)
    }

    operator fun set(index: Int, value: Any) {
        RefArray.set(array, index, value)
    }

    override fun iterator(): Iterator<Any?> {
        return JavaArrayIterator(this)
    }

    fun dump() {
        printX("Array<", elementClass, "> ", "length=", length)
    }

    class JavaArrayIterator(private val javaArray: JavaArray) : Iterator<Any?> {
        private var index: Int = 0

        override fun next(): Any? {
            return javaArray[index++]
        }

        override fun hasNext(): Boolean {
            return index < javaArray.length
        }

    }

    companion object {
        fun isArray(value: Any): Boolean = value.javaClass.isArray

        fun createArray(elementClass: KClass<*>, length: Int): Any {
            return RefArray.newInstance(elementClass.java, length)
        }

        fun create(elementClass: KClass<*>, length: Int): JavaArray {
            val a = RefArray.newInstance(elementClass.java, length)
            return JavaArray(a)
        }
    }
}

fun main() {
    val b = arrayOf(1, 2, 3)
    val a = Array<String>(2) { "" }

    JavaArray(a).dump()

}