package io.github.yangentao.types

import java.sql.Array
import java.sql.ResultSet
import kotlin.reflect.KProperty

fun <T> ResultSet.mapList(block: ResultSet.() -> T?): List<T> {
    val ls = ArrayList<T>(128)
    this.use { rs ->
        while (rs.next()) {
            block(rs)?.also { v -> ls.add(v) }
        }
    }
    return ls
}

fun <T : Any> Array.toList(block: (ResultSet) -> T): List<T> {
    val ls = this.resultSet.mapList(block)
    this.free()
    return ls
}

inline fun <reified T : Any> Array.toList(prop: KProperty<T>): List<T> {
    return this.toList { row ->
        val v = row.getObject(2)
        prop.decodeValueTyped(v) as T
    }
}

val Array.listBool: List<Boolean> get() = this.toList { it.getBoolean(2) }
val Array.listByte: List<Byte> get() = this.toList { it.getByte(2) }
val Array.listShort: List<Short> get() = this.toList { it.getShort(2) }
val Array.listInt: List<Int> get() = this.toList { it.getInt(2) }
val Array.listLong: List<Long> get() = this.toList { it.getLong(2) }
val Array.listFloat: List<Float> get() = this.toList { it.getFloat(2) }
val Array.listDouble: List<Double> get() = this.toList { it.getDouble(2) }
val Array.listString: List<String> get() = this.toList { it.getString(2) }
val Array.listChar: List<Char> get() = this.toList { it.getString(2).first() }
val Array.listObject: List<Any?> get() = this.toList { it.getObject(2) }