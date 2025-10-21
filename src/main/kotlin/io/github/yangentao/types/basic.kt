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
import java.util.concurrent.atomic.AtomicBoolean
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

open class DeamonThread(task: Runnable? = null, name: String? = "DeamonThread", deamon: Boolean = true, priority: Int = Thread.NORM_PRIORITY) : Thread(task, name) {
    init {
        this.isDaemon = deamon
        this.priority = priority
        setUncaughtExceptionHandler(::uncaughtException)
    }
}

internal fun uncaughtException(thread: Thread, ex: Throwable) {
    println("uncaughtException:  ${thread.name}")
    ex.printStackTrace()
}

fun interface OnException {
    fun onException(e: Throwable)
}

class SafeRunnable(val callback: Runnable, val onError: OnException? = null) : Runnable {
    override fun run() {
        try {
            callback.run()
        } catch (ex: Throwable) {
            ex.printStackTrace()
            onError?.onException(ex)
        }
    }
}

class LoopThread(val onLoop: Runnable, val onError: OnException? = null, val delay: Long = 10_000, name: String? = null) : Thread(name ?: "LoopThread") {

    private val running = AtomicBoolean(false)
    private val lock = Object()

    init {
        isDaemon = true
        priority = NORM_PRIORITY
    }

    val isRunning: Boolean get() = running.get()

    fun trigger() {
        synchronized(lock) {
            lock.notify()
        }
    }

    fun finish() {
        running.set(false)
        synchronized(lock) {
            lock.notify()
        }
    }

    override fun start() {
        running.set(true)
        super.start()
    }

    override fun run() {
        try {
            while (running.get()) {
                try {
                    onLoop.run()
                } catch (e: Throwable) {
                    e.printStackTrace()
                    onError?.onException(e)
                }
                if (delay > 0 && running.get()) {
                    synchronized(lock) {
                        lock.wait(delay)
                    }
                }
            }
        } finally {
            running.set(false)
        }
    }
}

fun interface OnValue<T> {
    fun onValue(value: T)
}

fun anyMap(vararg pairs: Pair<String, Any?>): Map<String, Any> {
    if (pairs.isEmpty()) return emptyMap()
    val map = LinkedHashMap<String, Any>()
    for (p in pairs) {
        p.second?.also {
            map[p.first] = it
        }
    }
    return map
}