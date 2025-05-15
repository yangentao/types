package io.github.yangentao.types

import io.github.yangentao.anno.*
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf

fun KProperty<*>.decodeValue2(source: Any?): Any? {
    val v = ValueDecoder.decodeValue(this.targetInfo, source)
    if (v != null || this.returnType.isMarkedNullable) return v
    error("null value, $this")
}

fun KParameter.decodeValue2(source: Any?): Any? {
    val v = ValueDecoder.decodeValue(this.targetInfo, source)
    if (v != null || this.type.isMarkedNullable || this.isOptional) return v
    error("null value, $this")
}

fun KClass<*>.decodeValue2(source: Any?): Any? {
    return ValueDecoder.decodeValue(this.targetInfo, source)
}

private typealias SQLArray = java.sql.Array

class TargetInfo(val clazz: KClass<*>, val annotations: List<Annotation>, val typeArguments: List<KType>) {
    val hasArguments: Boolean get() = typeArguments.isNotEmpty()

    val firstArg: KClass<*> get() = typeArguments.first().classifier as KClass<*>
    val secondArg: KClass<*> get() = typeArguments.second().classifier as KClass<*>

    inline fun <reified T : Annotation> findAnnotation(): T? {
        return annotations.firstOrNull { it is T } as? T
    }

    inline fun <reified T : Annotation> hasAnnotation(): Boolean {
        return annotations.any { it is T }
    }
}

val KProperty<*>.targetInfo: TargetInfo get() = TargetInfo(this.returnType.classifier as KClass<*>, this.annotations, this.returnType.arguments.map { it.type!! })
val KParameter.targetInfo: TargetInfo get() = TargetInfo(this.type.classifier as KClass<*>, this.annotations, this.type.arguments.map { it.type!! })
val KClass<*>.targetInfo: TargetInfo get() = TargetInfo(this, this.annotations, emptyList())

abstract class ValueDecoder() {

    abstract fun accept(target: KClass<*>, source: KClass<*>): Boolean
    abstract fun decode(targetInfo: TargetInfo, value: Any): Any?

    companion object {
        private val decoders: ArrayList<ValueDecoder> = arrayListOf(NumberDecoder, StringDecoder, BoolDecoder, CollectionDecoder, DateDecoder)

        fun push(decoder: ValueDecoder) {
            if (decoders.contains(decoder)) return
            decoders.add(0, decoder)
        }

        fun add(decoder: ValueDecoder) {
            if (decoders.contains(decoder)) return
            decoders.add(decoder)
        }

        fun decodeValue(target: TargetInfo, source: Any?): Any? {
            if (source == null) {
                val nullAnno: NullValue? = target.annotations.firstTyped()
                if (nullAnno != null) {
                    return decodeValue(target, nullAnno.value)
                }
                val f: String? = target.findAnnotation<ModelField>()?.defaultValue
                if (f != null) {
                    return decodeValue(target, f)
                }
                return null
            }
            val sourceClass = source::class
            if (!CollectionDecoder.accept(target.clazz, sourceClass)) {
                if (sourceClass == target.clazz) return source
                if (sourceClass.isSubclassOf(target.clazz)) return source
            }

            for (d in decoders) {
                if (d.accept(target.clazz, sourceClass)) {
                    return d.decode(target, source)
                }
            }
            error("NO decoder found! ${target.clazz}, $source")
        }
    }
}

private object StringDecoder : ValueDecoder() {
    override fun accept(target: KClass<*>, source: KClass<*>): Boolean {
        return target == String::class
    }

    override fun decode(targetInfo: TargetInfo, value: Any): Any? {
        when (value) {
            is Number -> {
                val nf: NumberPattern? = targetInfo.annotations.firstTyped()
                val sf: StringFormat? = targetInfo.annotations.firstTyped()
                if (nf != null) {
                    return value.format(nf.pattern)
                } else if (sf != null) {
                    return String.format(sf.pattern, value)
                } else {
                    return value.toString()
                }
            }

            is java.sql.Date -> return DateTime.from(value).formatDate()
            is java.sql.Time -> return DateTime.from(value).formatTime()
            is java.sql.Timestamp -> return DateTime.from(value).formatDateTime()

            is java.util.Date -> {
                val dp: DatePattern? = targetInfo.annotations.firstTyped()
                return if (dp != null) {
                    SimpleDateFormat(dp.format, Locale.getDefault()).format(value)
                } else {
                    DateTime.from(value).formatDate()
                }
            }

            is LocalDate -> {
                val dp: DatePattern? = targetInfo.annotations.firstTyped()
                return if (dp != null) {
                    DateTime.from(value).format(dp.format)
                } else {
                    DateTime.from(value).formatDate()
                }
            }

            is LocalTime -> return DateTime.from(value).formatTime()

            is LocalDateTime -> return DateTime.from(value).formatDateTime()
            else -> return value.toString()
        }
    }

}

private object NumberDecoder : ValueDecoder() {
    override fun accept(target: KClass<*>, source: KClass<*>): Boolean {
        if (!target.isSubclassOf(Number::class)) return false
        if (source == String::class || source.isSubclassOf(Number::class)) return true
        if (source.isSubclassOf(java.util.Date::class)) return true
        return false
    }

    override fun decode(targetInfo: TargetInfo, value: Any): Number? {
        when (value) {
            is java.util.Date -> return value.time
            is DateTime -> return value.timeInMillis
            is LocalDate -> return DateTime.from(value).timeInMillis
            is LocalTime -> return DateTime.from(value).timeInMillis
            is LocalDateTime -> return DateTime.from(value).timeInMillis
        }
        return when (targetInfo.clazz) {
            Byte::class -> if (value is String) value.toByte() else (value as Number).toByte()
            Short::class -> if (value is String) value.toShort() else (value as Number).toShort()
            Int::class -> if (value is String) value.toInt() else (value as Number).toInt()
            Long::class -> if (value is String) value.toLong() else (value as Number).toLong()
            Float::class -> if (value is String) value.toFloat() else (value as Number).toFloat()
            Double::class -> if (value is String) value.toDouble() else (value as Number).toDouble()
            else -> error("NOT support type: ${targetInfo.clazz}")
        }

    }
}

private object BoolDecoder : ValueDecoder() {
    override fun accept(target: KClass<*>, source: KClass<*>): Boolean {
        return target == Boolean::class && (source == String::class || source.isSubclassOf(Number::class))
    }

    override fun decode(targetInfo: TargetInfo, value: Any): Boolean? {
        return when (value) {
            is String -> value.toBoolean()
            is Number -> value.toInt() == 1
            else -> error("NOT support type: ${targetInfo.clazz}")
        }
    }
}

private object CollectionDecoder : ValueDecoder() {
    override fun accept(target: KClass<*>, source: KClass<*>): Boolean {
        return target.isSubclassOf(Collection::class) || target.isSubclassOf(Map::class)
    }

    private fun decodeList(targetInfo: TargetInfo, value: Any): Any? {
        when (value) {
            is SQLArray -> {
                val ls = ArrayList<Any?>()
                value.resultSet.use {
                    while (it.next()) {
                        ls.add(it.getObject(2))
                    }
                }
                value.free()
                return ls
            }

            is Iterable<*> -> {
                if (targetInfo.hasArguments) {
                    val ls = ArrayList<Any?>()
                    val ti = TargetInfo(targetInfo.firstArg, targetInfo.annotations, emptyList())
                    for (v in value) {
                        val vv = ValueDecoder.decodeValue(ti, v)
                        ls.add(vv)
                    }
                    return ls
                } else {
                    if (value::class.isSubclassOf(targetInfo.clazz)) {
                        return value
                    } else {
                        val ls = ArrayList<Any?>()
                        ls.addAll(value)
                        return ls
                    }
                }

            }

            is String -> {
                val sc: SepChar? = targetInfo.findAnnotation()
                val ch: Char = sc?.list ?: ','
                val sls = value.split(ch)
                return this.decodeList(targetInfo, sls)
            }

            else -> error("Not support type: ${targetInfo.clazz}, $value")
        }
    }

    private fun decodeSet(targetInfo: TargetInfo, value: Any): Any? {
        when (value) {
            is Iterable<*> -> {

                if (targetInfo.hasArguments) {
                    val aSet = HashSet<Any?>()
                    val ti = TargetInfo(targetInfo.firstArg, targetInfo.annotations, emptyList())
                    for (v in value) {
                        val vv = ValueDecoder.decodeValue(ti, v)
                        aSet.add(vv)
                    }
                    return aSet
                } else {
                    if (value::class.isSubclassOf(targetInfo.clazz)) {
                        return value
                    } else {
                        val aSet = HashSet<Any?>()
                        aSet.addAll(value)
                        return aSet
                    }
                }
            }

            is String -> {
                val sc: SepChar? = targetInfo.findAnnotation()
                val ch: Char = sc?.list ?: ','
                val sls = value.split(ch)
                return this.decodeSet(targetInfo, sls)
            }

            else -> error("Not support type: ${targetInfo.clazz}, $value")
        }
    }

    private fun decodeMap(targetInfo: TargetInfo, value: Any): Any? {
        when (value) {
            is Map<*, *> -> {

                if (targetInfo.typeArguments.isNotEmpty()) {
                    val aMap = HashMap<Any, Any?>()
                    val tKey = TargetInfo(targetInfo.firstArg, targetInfo.annotations, emptyList())
                    val tVal = TargetInfo(targetInfo.secondArg, targetInfo.annotations, emptyList())
                    for (e in value) {
                        val vK = ValueDecoder.decodeValue(tKey, e.key)
                        val vv = ValueDecoder.decodeValue(tVal, e.value)
                        if (vK != null) aMap.put(vK, vv)
                    }
                    return aMap
                } else {
                    if (value::class.isSubclassOf(targetInfo.clazz)) {
                        return value
                    }
                    val aMap = HashMap<Any, Any?>()
                    for (e in value) {
                        aMap.put(e.key!!, e.value)
                    }
                    return aMap
                }

            }

            is String -> {
                val sc: SepChar = targetInfo.findAnnotation() ?: SepChar()
                val aMap = HashMap<String, String?>()
                val sls = value.split(sc.list)
                for (s in sls) {
                    val pair = s.split(sc.map)
                    if (pair.size == 2) {
                        aMap.put(pair.first(), pair.second())
                    }
                }
                return decodeMap(targetInfo, aMap)
            }

            else -> error("Not support type: ${targetInfo.clazz}, $value")
        }
    }

    override fun decode(targetInfo: TargetInfo, value: Any): Any? {
        return when (targetInfo.clazz) {
            List::class, ArrayList::class -> decodeList(targetInfo, value)
            Set::class, HashSet::class -> decodeSet(targetInfo, value)
            Map::class, HashMap::class -> decodeMap(targetInfo, value)
            else -> error("Not support type: ${targetInfo.clazz}, $value ")
        }
    }

}

private object DateDecoder : ValueDecoder() {
    private val clsSet: Set<KClass<*>> = setOf(
        java.util.Date::class, java.sql.Date::class, java.sql.Time::class, Timestamp::class,
        LocalDate::class, LocalTime::class, LocalDateTime::class
    )

    override fun accept(target: KClass<*>, source: KClass<*>): Boolean {
        return target in clsSet && (source in clsSet || source == Long::class || source == String::class)
    }

    private fun toDateTime(info: TargetInfo, value: Any): DateTime {
        when (value) {
            is java.sql.Date -> return DateTime.from(value)
            is java.sql.Time -> return DateTime.from(value)
            is java.sql.Timestamp -> return DateTime.from(value)
            is java.util.Date -> return DateTime.from(value)
            is LocalDate -> return DateTime.from(value)
            is LocalTime -> return DateTime.from(value)
            is LocalDateTime -> return DateTime.from(value)
            is Long -> return DateTime(value)
            is String -> {
                val dp: DatePattern? = info.findAnnotation()
                return if (dp != null) {
                    DateTime.parse(dp.format, value) ?: error("Parse error, ${info.clazz},  $value")
                } else {
                    DateTime.parseDate(value) ?: DateTime.parseDateTime(value) ?: DateTime.parseTime(value) ?: error("Parse error, ${info.clazz},  $value")
                }
            }

            else -> error("Unsupport type, ${info.clazz},  $value")
        }
    }

    override fun decode(targetInfo: TargetInfo, value: Any): Any? {
        val dt = toDateTime(targetInfo, value)
        return when (targetInfo.clazz) {
            java.util.Date::class -> dt.date
            java.sql.Date::class -> dt.dateSQL
            java.sql.Time::class -> dt.time
            java.sql.Timestamp::class -> dt.timestamp
            LocalDate::class -> dt.localDate
            LocalTime::class -> dt.localTime
            LocalDateTime::class -> dt.localDateTime
            else -> error("NOT support type: ${targetInfo.clazz}")
        }
    }
}

