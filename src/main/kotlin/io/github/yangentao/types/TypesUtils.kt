package io.github.yangentao.types

@Suppress("UNCHECKED_CAST")
internal inline fun <reified T> Collection<Any>.firstTyped(): T? {
    return this.firstOrNull { it is T } as? T
}

fun printX(vararg vs: Any?) {
    val s = vs.joinToString(" ") {
        it?.toString() ?: "null"
    }
    println(s)
}