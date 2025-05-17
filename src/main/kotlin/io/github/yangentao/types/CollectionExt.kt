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
    return this.filter { (it is T) && predicate(it) } as List<T>
}

@Deprecated("Use listTyped()")
@Suppress("UNCHECKED_CAST")
inline fun <reified T> Collection<Any>.filterTyped(): List<T> {
    return this.filter { it is T } as List<T>
}

@Deprecated("Use typed() instead.")
@Suppress("UNCHECKED_CAST")
inline fun <reified T> Collection<Any>.firstTyped(): T? {
    return this.firstOrNull { it is T } as? T
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> Collection<Any>.listTyped(): List<T> {
    return this.filter { it is T } as List<T>
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> Collection<Any>.typed(): T? {
    return this.firstOrNull { it is T } as? T
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

@Deprecated("Use secondOr() instead.")
fun <T> List<T>.secondOrNull(): T? {
    return this.getOrNull(1)
}

fun <T> List<T>.secondOr(): T? {
    return this.getOrNull(1)
}

fun <T> List<T>.firstOr(): T? {
    return this.firstOrNull()
}

fun <T : Any> Stack<T>.top(): T? {
    if (empty()) return null
    return peek()
}

fun <T : Any> Stack<T>.popX(): T? {
    if (empty()) return null
    return pop()
}

inline fun <K, V> MutableMap<K, V>.getOrPutX(key: K, defaultValue: () -> V?): V? {
    val value = get(key)
    return if (value == null) {
        val answer = defaultValue()
        if (answer != null) put(key, answer)
        answer
    } else {
        value
    }
}

class LongCache<T : Any>(private val finder: (Long) -> T?) {
    private val map = HashMap<Long, T?>()
    private val nullSet = HashSet<Long>()

    fun get(key: Long): T? {
        if (nullSet.contains(key)) return null
        return map.getOrPutX(key) {
            val v = finder(key)
            if (v == null) nullSet.add(key)
            v
        }
    }
}

class ItemCache<T : Any>(private val finder: (Int) -> T?) {
    private val map = HashMap<Int, T?>()
    private val nullSet = HashSet<Int>()

    fun get(key: Int): T? {
        if (nullSet.contains(key)) return null
        return map.getOrPutX(key) {
            val v = finder(key)
            if (v == null) nullSet.add(key)
            v
        }
    }
}
typealias LongStringCache = ValueCache<Long, String>

class ValueCache<K : Any, V : Any>(private val finder: (K) -> V?) {
    private val map = HashMap<K, V>()
    private val nullSet = HashSet<K>()

    operator fun get(key: K): V? {
        if (nullSet.contains(key)) return null
        return map.getOrPutX(key) {
            val v = finder(key)
            if (v == null) nullSet.add(key)
            v
        }
    }
}

class MapCache<K : Any, V : Any>(private val finder: (K) -> V?) {
    private val map = HashMap<K, V>()
    private val nullSet = HashSet<K>()

    fun get(key: K): V? {
        if (nullSet.contains(key)) return null
        if (map.containsKey(key)) return map[key]
        val v = finder(key)
        if (v == null) {
            nullSet.add(key)
        } else {
            map[key] = v
        }
        return v
    }
}
