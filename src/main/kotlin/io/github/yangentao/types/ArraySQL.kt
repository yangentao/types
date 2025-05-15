package io.github.yangentao.types

import java.sql.ResultSet
import kotlin.reflect.KProperty

typealias ArraySQL = java.sql.Array

fun <T> ResultSet.mapList(block: ResultSet.() -> T?): List<T> {
    val ls = ArrayList<T>(128)
    this.use { rs ->
        while (rs.next()) {
            block(rs)?.also { v -> ls.add(v) }
        }
    }
    return ls
}

fun <T : Any> ArraySQL.toList(block: (ResultSet) -> T): List<T> {
    val ls = this.resultSet.mapList(block)
    this.free()
    return ls
}

inline fun <reified T : Any> ArraySQL.toList(prop: KProperty<T>): List<T> {
    return this.toList { row ->
        val v = row.getObject(2)
        prop.decodeValue(v) as T
    }
}

fun ArraySQL.listAny(freeMe: Boolean = true): List<Any?> {
    val ls = ArrayList<Any?>()
    this.resultSet.use {
        while (it.next()) {
            ls.add(it.getObject(2))
        }
    }
    if (freeMe) this.free()
    return ls
}