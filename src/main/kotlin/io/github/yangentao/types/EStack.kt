package io.github.yangentao.types

import java.util.*

class EStack<T : Any>(val list: LinkedList<T> = LinkedList()) : Iterable<T> by list {

    val size: Int get() = list.size
    val isEmpty: Boolean get() = list.isEmpty()
    val isNotEmpty: Boolean get() = list.isNotEmpty()

    fun push(value: T) {
        list.push(value)
    }

    fun pop(): T {
        return list.pop()
    }

    fun peek(): T? {
        return list.peek()
    }

    fun clear() {
        list.clear()
    }

    override fun toString(): String {
        return "EStack[${this.joinToString(", ")}]"
    }

}
