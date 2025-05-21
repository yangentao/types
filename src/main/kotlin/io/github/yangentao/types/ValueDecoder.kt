@file:Suppress("RemoveRedundantQualifierName")

package io.github.yangentao.types

import io.github.yangentao.anno.*
import io.github.yangentao.kson.KsonArray
import io.github.yangentao.kson.KsonNum
import io.github.yangentao.kson.KsonObject
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
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.isSubclassOf

fun KProperty<*>.decodeValue(source: Any?): Any? {
    val v = ValueDecoder.decodeValue(this.targetInfo, source)
    if (v != null || this.returnType.isMarkedNullable) return v
    error("null value, $this")
}

fun KParameter.decodeValue(source: Any?): Any? {
    val v = ValueDecoder.decodeValue(this.targetInfo, source)
    if (v != null || this.type.isMarkedNullable || this.isOptional) return v
    error("null value, $this")
}

fun KClass<*>.decodeValue(source: Any?): Any? {
    return ValueDecoder.decodeValue(this.targetInfo, source)
}

class TargetInfo(val clazz: KClass<*>, val annotations: List<Annotation> = emptyList(), val typeArguments: List<KType> = emptyList()) {
    val hasArguments: Boolean get() = typeArguments.isNotEmpty()

    val firstArg: KClass<*>? get() = typeArguments.firstOr()?.classifier as KClass<*>
    val secondArg: KClass<*>? get() = typeArguments.secondOr()?.classifier as KClass<*>

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
        private val decoders: ArrayList<ValueDecoder> = arrayListOf(
            NumberDecoder, StringDecoder, BoolDecoder,
            ListDecoder, SetDecoder, MapDecoder,
            DateDecoder, ArrayDecoder
        )

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
                val nullAnno: NullValue? = target.annotations.typed()
                if (nullAnno != null) {
                    return decodeValue(target, nullAnno.value)
                }
                val f: String? = target.findAnnotation<ModelField>()?.defaultValue
                if (f != null && f.isNotEmpty()) {
                    return decodeValue(target, f)
                }
                return null
            }
            val sourceClass = source::class
            if (sourceClass == target.clazz) return source
//            if (sourceClass.isSubclassOf(target.clazz)) return source

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
                val nf: NumberPattern? = targetInfo.findAnnotation()
                val sf: StringFormat? = targetInfo.findAnnotation()
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
                val dp: DatePattern? = targetInfo.annotations.typed()
                return if (dp != null) {
                    SimpleDateFormat(dp.format, Locale.getDefault()).format(value)
                } else {
                    DateTime.from(value).formatDate()
                }
            }

            is LocalDate -> {
                val dp: DatePattern? = targetInfo.annotations.typed()
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
        return target.isSubclassOf(Number::class)
    }

    override fun decode(targetInfo: TargetInfo, value: Any): Number? {
        when (value) {
            is java.util.Date -> return value.time
            is DateTime -> return value.timeInMillis
            is LocalDate -> return DateTime.from(value).timeInMillis
            is LocalTime -> return DateTime.from(value).timeInMillis
            is LocalDateTime -> return DateTime.from(value).timeInMillis
        }
        val numValue: Any = if (value is KsonNum) value.data else value
        return when (targetInfo.clazz) {
            Byte::class -> if (numValue is String) numValue.toByte() else (numValue as Number).toByte()
            Short::class -> if (numValue is String) numValue.toShort() else (numValue as Number).toShort()
            Int::class -> if (numValue is String) numValue.toInt() else (numValue as Number).toInt()
            Long::class -> if (numValue is String) numValue.toLong() else (numValue as Number).toLong()
            Float::class -> if (numValue is String) numValue.toFloat() else (numValue as Number).toFloat()
            Double::class -> if (numValue is String) numValue.toDouble() else (numValue as Number).toDouble()
            else -> error("NOT support type: ${targetInfo.clazz}")
        }

    }
}

private object BoolDecoder : ValueDecoder() {
    val trueList = listOf("true", "yes", "1")
    val falseList = listOf("false", "no", "0")

    fun toBool(s: String): Boolean? {
        for (a in trueList) {
            if (a ieq s) return true
        }
        for (a in falseList) {
            if (a ieq s) return false
        }
        return null
    }

    override fun accept(target: KClass<*>, source: KClass<*>): Boolean {
        return target == Boolean::class && (source == String::class || source.isSubclassOf(Number::class))
    }

    override fun decode(targetInfo: TargetInfo, value: Any): Boolean? {
        return when (value) {
            is String -> toBool(value)
            is Number -> if (value.toInt() == 1) true else if (value.toInt() == 0) false else null
            else -> error("NOT support type: ${targetInfo.clazz}")
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

    private fun toDateTime(info: TargetInfo, value: Any): DateTime? {
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
                if (value.isEmpty()) return null
                val dp: DatePattern? = info.findAnnotation()
                return if (dp != null) {
                    DateTime.parse(dp.format, value) ?: error("Parse error, ${info.clazz},  $value")
                } else {
                    DateTime.parseDate(value) ?: DateTime.parseDateTime(value) ?: DateTime.parseTime(value) ?: error("Parse error, ${info.clazz},  value='$value'")
                }
            }

            else -> error("Unsupport type, ${info.clazz},  $value")
        }
    }

    override fun decode(targetInfo: TargetInfo, value: Any): Any? {
        val dt = toDateTime(targetInfo, value) ?: return null
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

object ArrayDecoder : ValueDecoder() {
    override fun accept(target: KClass<*>, source: KClass<*>): Boolean {
        return target.java.isArray
    }

    override fun decode(targetInfo: TargetInfo, value: Any): Any? {
        val eleCls = targetInfo.clazz.java.componentType
        val ls: List<Any?> = prepareItems(value, targetInfo.findAnnotation()).toList()
        val ar = java.lang.reflect.Array.newInstance(eleCls, ls.size)
        for (i in ls.indices) {
            val v = decodeValue(eleCls.kotlin.targetInfo, ls[i])
            java.lang.reflect.Array.set(ar, i, v)
        }
        return ar
    }

}

private object ListDecoder : ValueDecoder() {
    override fun accept(target: KClass<*>, source: KClass<*>): Boolean {
        return target.isSubclassOf(List::class)
    }

    override fun decode(targetInfo: TargetInfo, value: Any): Any? {
        val eleType: KClass<*> = targetInfo.firstArg ?: return null
        val ls: Iterable<Any?> = prepareItems(value, targetInfo.findAnnotation())
        val valueList: List<Any?> = ls.map { decodeValue(TargetInfo(eleType, targetInfo.annotations, emptyList()), it) }
        if (!targetInfo.clazz.isAbstract && targetInfo.clazz.isSubclassOf(MutableList::class)) {
            @Suppress("UNCHECKED_CAST")
            val muList: MutableList<Any?> = targetInfo.clazz.createInstance() as MutableList<Any?>
            muList.addAll(valueList)
            return muList
        }
        return ArrayList(valueList)
    }

}

private object SetDecoder : ValueDecoder() {
    override fun accept(target: KClass<*>, source: KClass<*>): Boolean {
        return target.isSubclassOf(Set::class)
    }

    override fun decode(targetInfo: TargetInfo, value: Any): Any? {
        val eleType: KClass<*> = targetInfo.firstArg ?: return null
        val ls: Iterable<Any?> = prepareItems(value, targetInfo.findAnnotation())
        val valueList = ls.map { decodeValue(TargetInfo(eleType, targetInfo.annotations, emptyList()), it) }
        if (!targetInfo.clazz.isAbstract && targetInfo.clazz.isSubclassOf(MutableSet::class)) {
            @Suppress("UNCHECKED_CAST")
            val muList: MutableSet<Any?> = targetInfo.clazz.createInstance() as MutableSet<Any?>
            muList.addAll(valueList)
            return muList
        }
        return LinkedHashSet(valueList)
    }

}

private object MapDecoder : ValueDecoder() {
    override fun accept(target: KClass<*>, source: KClass<*>): Boolean {
        return target.isSubclassOf(Map::class)
    }

    override fun decode(targetInfo: TargetInfo, value: Any): Any? {
        if (targetInfo.typeArguments.size < 2) return null
        val keyType: KClass<*> = targetInfo.firstArg!!
        val valType: KClass<*> = targetInfo.secondArg!!
        val valueMap: LinkedHashMap<Any, Any?> = LinkedHashMap<Any, Any?>()
        if (value is Map<*, *>) {
            for ((k, v) in value) {
                val vv = decodeValue(TargetInfo(valType, targetInfo.annotations), v)
                valueMap[k as Any] = vv
            }
        } else if (value is String) {
            val mapChars: SepChar? = targetInfo.findAnnotation()
            val pls = value.split(mapChars?.list ?: ',').map { it.split(mapChars?.map ?: ':') }
            for (p in pls) {
                if (p.size == 2) {
                    val k = decodeValue(TargetInfo(keyType, targetInfo.annotations), p.first().trim()) ?: continue
                    val v = decodeValue(TargetInfo(valType, targetInfo.annotations), p.second().trim()) ?: continue
                    valueMap[k] = v
                }
            }
        } else {
            error("Type  dismatch $value ")
        }
        if (!targetInfo.clazz.isAbstract && targetInfo.clazz.isSubclassOf(MutableMap::class)) {
            @Suppress("UNCHECKED_CAST")
            val muMap: MutableMap<Any, Any?> = targetInfo.clazz.createInstance() as MutableMap<Any, Any?>
            muMap.putAll(valueMap)
            return muMap
        }
        return valueMap
    }

}

private fun prepareItems(value: Any, sepChar: SepChar? = null): Iterable<Any?> {
    return when (value) {
        is Set<*> -> value
        is List<*> -> value
        is Array<*> -> value.toList()
        is java.sql.Array -> value.listAny(true)

        is String -> value.split(sepChar?.list ?: ',').map { it.trim() }

        is ByteArray -> value.toList()
        is ShortArray -> value.toList()
        is IntArray -> value.toList()
        is LongArray -> value.toList()
        is FloatArray -> value.toList()
        is DoubleArray -> value.toList()
        is BooleanArray -> value.toList()
        is CharArray -> value.toList()

        else -> error("Type  dismatch $value ")
    }
}

private object KsonObjectDecoder : ValueDecoder() {
    override fun accept(target: KClass<*>, source: KClass<*>): Boolean {
        return target == KsonObject::class
    }

    override fun decode(targetInfo: TargetInfo, value: Any): Any? {
        return when (value) {
            is String -> KsonObject(value)
            else -> error("unknown type: $value ")
        }
    }
}

private object KsonArrayDecoder : ValueDecoder() {
    override fun accept(target: KClass<*>, source: KClass<*>): Boolean {
        return target == KsonArray::class
    }

    override fun decode(targetInfo: TargetInfo, value: Any): Any? {
        return when (value) {
            is String -> KsonArray(value)
            else -> error("unknown type: $value ")
        }
    }
}
