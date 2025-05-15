package io.github.yangentao.types

import io.github.yangentao.anno.DatePattern
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

typealias Predicater<T> = (T) -> Boolean

val javaVersionInt: Int = System.getProperty("java.specification.version")?.toIntOrNull() ?: 0

@Suppress("Since15")
val Thread.tid: Long get() = if (javaVersionInt >= 19) this.threadId() else this.id

@Suppress("RecursivePropertyAccessor")
val Throwable.rootError: Throwable
    get() {
        return this.cause?.rootError ?: this
    }

fun printX(vararg vs: Any?) {
    val s = vs.joinToString(" ") {
        it?.toString() ?: "null"
    }
    println(s)
}

inline fun <R> safe(block: () -> R): R? {
    try {
        return block()
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
    return null
}

val Throwable.stackInfo: String
    get() {
        val w = StringWriter(1024)
        val p = PrintWriter(w)
        this.printStackTrace(p)
        p.flush()
        return w.toString()
    }

fun DatePattern.display(v: Any): String {
    return dateDisplay(v, this.format)
}

fun dateDisplay(v: Any, format: String): String {
    //java.util.Date包含java.sql.Date和Timestamp,Time
    return when (v) {
        is java.util.Date -> SimpleDateFormat(format, Locale.getDefault()).format(v)
        is Long -> SimpleDateFormat(format, Locale.getDefault()).format(java.util.Date(v))
        is DateTime -> v.format(format)
        is LocalDate -> v.format(format)
        is LocalDateTime -> v.format(format)
        is LocalTime -> v.format(format)
        else -> v.toString()
    }
}

inline fun <reified T : Comparable<T>> T.greatEqual(v: T): T {
    if (this < v) return v
    return this
}

inline fun <reified T : Comparable<T>> T.lessEqual(v: T): T {
    if (this > v) return v
    return this
}