package io.github.yangentao.types

import io.github.yangentao.anno.DatePattern
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*
import kotlin.reflect.KClass

typealias Predicater<T> = (T) -> Boolean

val javaVersionInt: Int = System.getProperty("java.specification.version")?.toIntOrNull() ?: 0

@Suppress("Since15", "DEPRECATION")
val Thread.tid: Long get() = if (javaVersionInt >= 19) this.threadId() else this.id

val Thread.isMain: Boolean get() = this.tid == 1L

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

data class LabelValue<T : Any>(val label: String, val value: T)

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

val Int.fileSize: String get() = this.toLong().fileSize

val Long.fileSize: String
    get() {
        return when {
            this > GB -> (this * 1.0 / GB).maxFraction(2) + "G"
            this > MB -> (this * 1.0 / MB).maxFraction(2) + "M"
            this > KB -> (this * 1.0 / KB).maxFraction(2) + "K"
            else -> this.toString() + "字节"
        }
    }
val UUID.hexText: String get() = String.format("%x%x", this.mostSignificantBits, this.leastSignificantBits)

fun File.ensureDirs(): File {
    if (!this.exists()) {
        this.mkdirs()
    }
    return this
}

fun KClass<*>.resourceBytes(name: String): ByteArray? {
    val i = this.java.classLoader.getResourceAsStream(name) ?: return null
    i.use {
        return it.readBytes()
    }
}

fun KClass<*>.resourceText(name: String): String? {
    val i = this.java.classLoader.getResourceAsStream(name) ?: return null
    i.use {
        return it.readBytes().toString(Charsets.UTF_8)
    }
}

class DeamonThread(r: Runnable) : Thread(r) {
    init {
        this.isDaemon = true
    }
}
