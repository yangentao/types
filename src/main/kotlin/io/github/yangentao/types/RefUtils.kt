package io.github.yangentao.types

import java.text.DecimalFormat

@Suppress("UNCHECKED_CAST")
internal inline fun <reified T> Collection<Any>.firstTyped(): T? {
    return this.firstOrNull { it is T } as? T
}

//12345.format(",###.##")
//12345.6789.format("0,000.00")
//@see DecimalFormat
internal fun Number.format(pattern: String): String {
    return if (pattern.isEmpty()) {
        this.toString()
    } else {
        DecimalFormat(pattern).format(this)
    }
}

internal fun Number.format(integers: Int, fractions: Int): String {
    val df = DecimalFormat()
    df.isGroupingUsed = false
    if (integers > 0) df.minimumIntegerDigits = integers
    if (fractions > 0) {
        df.minimumFractionDigits = fractions
        df.maximumFractionDigits = fractions
    }
    return df.format(this)
}