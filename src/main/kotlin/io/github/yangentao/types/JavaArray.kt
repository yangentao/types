package io.github.yangentao.types

import kotlin.reflect.KClass

class JavaArray(val array: Any) : Iterable<Any> {
    val elementClass: KClass<*> get() = this.array::class.java.componentType.kotlin
    val length: Int = java.lang.reflect.Array.getLength(array)
    val isEmpty: Boolean get() = length == 0
    val isNotEmpty: Boolean get() = length > 0

    operator fun get(index: Int): Any {
        return java.lang.reflect.Array.get(array, index)
    }

    operator fun set(index: Int, value: Any) {
        java.lang.reflect.Array.set(array, index, value)
    }

    override fun iterator(): Iterator<Any> {
        return JavaArrayIterator(this)
    }

    class JavaArrayIterator(private val javaArray: JavaArray) : Iterator<Any> {
        private var index: Int = 0

        override fun next(): Any {
            return javaArray[index++]
        }

        override fun hasNext(): Boolean {
            return index < javaArray.length
        }

    }

    companion object {
        fun isArray(value: Any): Boolean = value::class.java.isArray

        fun createArray(elementClass: KClass<*>, length: Int): Any {
            return java.lang.reflect.Array.newInstance(elementClass.java, length)
        }
    }
}