package io.github.yangentao.types

import java.util.*

operator fun <K, V> MutableMap<K, V>.plusAssign(pair: Pair<K, V>) {
    this[pair.first] = pair.second
}

fun <T, R> Iterable<T>.intersectBy(other: Iterable<T>, block: (T) -> R): List<T> {
    val ls = ArrayList<T>()
    for (a in this) {
        val ok = other.any { block(it) == block(a) }
        if (ok) {
            ls.add(a)
        }
    }
    return ls
}

@Suppress("UNCHECKED_CAST")
inline fun <reified E, reified T> Collection<E>.filterTyped(predicate: (E) -> Boolean): List<T> {
    return this.filter { predicate(it) && (it is T) } as List<T>
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> Collection<Any>.filterTyped(): List<T> {
    return this.filter { it is T } as List<T>
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> Collection<Any>.firstTyped(): T? {
    return this.first { it is T } as? T
}

@Suppress("UNCHECKED_CAST")
fun <B : Any> Collection<*>.cast(): List<B> {
    return this.map { it as B }
}

fun <T> List<T>.sublist(from: Int): List<T> {
    return this.subList(from, size)
}

fun <T> MutableList<T>.shift(n: Int) {
    if (n in 1..this.size) {
        for (i in 1..n) {
            this.removeAt(0)
        }
    }
}

fun <T> List<T>.exists(p: Predicater<T>): Boolean {
    return this.firstOrNull(p) != null
}

fun <T> List<T>.second(): T {
    return this[1]
}

fun <T> List<T>.secondOrNull(): T? {
    return this.getOrNull(1)
}

fun <T : Any> Stack<T>.top(): T? {
    if (empty()) return null
    return peek()
}

fun <T : Any> Stack<T>.popX(): T? {
    if (empty()) return null
    return pop()
}
