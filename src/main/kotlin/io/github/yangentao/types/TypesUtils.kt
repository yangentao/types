package io.github.yangentao.types

val javaVersionInt: Int = System.getProperty("java.specification.version")?.toIntOrNull() ?: 0

@Suppress("Since15")
val Thread.tid: Long get() = if (javaVersionInt >= 19) this.threadId() else this.id

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