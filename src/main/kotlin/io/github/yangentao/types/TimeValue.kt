package io.github.yangentao.types

import java.util.concurrent.TimeUnit

data class TimeValue(val unit: TimeUnit, val value: Long) {

    val toMilliSeconds: TimeValue get() = TimeValue(TimeUnit.MILLISECONDS, TimeUnit.MILLISECONDS.convert(value, unit))
    val toSeconds: TimeValue get() = TimeValue(TimeUnit.SECONDS, TimeUnit.SECONDS.convert(value, unit))
    val toMinutes: TimeValue get() = TimeValue(TimeUnit.MINUTES, TimeUnit.MINUTES.convert(value, unit))
    val toHours: TimeValue get() = TimeValue(TimeUnit.HOURS, TimeUnit.HOURS.convert(value, unit))
    val toDays: TimeValue get() = TimeValue(TimeUnit.DAYS, TimeUnit.DAYS.convert(value, unit))
    fun toTimeUnit(newTimeUnit: TimeUnit): TimeValue {
        return TimeValue(newTimeUnit, newTimeUnit.convert(value, unit))
    }

    val inMillSeconds: Long get() = TimeUnit.MILLISECONDS.convert(value, unit)
    val inSeconds: Long get() = TimeUnit.SECONDS.convert(value, unit)
    val inMinutes: Long get() = TimeUnit.MINUTES.convert(value, unit)
    val inHours: Long get() = TimeUnit.HOURS.convert(value, unit)
    val inDays: Long get() = TimeUnit.DAYS.convert(value, unit)

    companion object {
        fun milliseconds(value: Long): TimeValue {
            return TimeValue(TimeUnit.MILLISECONDS, value)
        }

        fun seconds(value: Long): TimeValue {
            return TimeValue(TimeUnit.SECONDS, value)
        }

        fun minutes(value: Long): TimeValue {
            return TimeValue(TimeUnit.MINUTES, value)
        }

        fun hours(value: Long): TimeValue {
            return TimeValue(TimeUnit.HOURS, value)
        }

        fun days(value: Long): TimeValue {
            return TimeValue(TimeUnit.DAYS, value)
        }
    }
}

val Number.timeMilliSeconds: TimeValue get() = TimeValue(TimeUnit.MILLISECONDS, this.toLong())
val Number.timeSeconds: TimeValue get() = TimeValue(TimeUnit.SECONDS, this.toLong())
val Number.timeMinutes: TimeValue get() = TimeValue(TimeUnit.MINUTES, this.toLong())
val Number.timeHours: TimeValue get() = TimeValue(TimeUnit.HOURS, this.toLong())
val Number.timeDays: TimeValue get() = TimeValue(TimeUnit.DAYS, this.toLong())

private const val YEAR_MILL: Long = 365 * 24 * 3600_000L
private const val DAY_MILL: Long = 24 * 3600_000L
fun TimeValue.format(): String {
    val sb = StringBuilder()
    var tm = this.inMillSeconds
    if (tm == 0L) {
        sb.append("0毫秒")
    } else {
        val years = tm / YEAR_MILL
        tm %= YEAR_MILL
        val days = tm / DAY_MILL
        tm %= DAY_MILL
        val hours = tm / 3600_000L
        tm %= 3600_000L
        val minutes = tm / 60_000L
        tm %= 60_000L
        val seconds = tm / 1_000L
        val millSec = tm % 1000L
        if (years > 0) sb.append("${years}年")
        if (days > 0) sb.append("${days}天")
        if (hours > 0) sb.append("${hours}时")
        if (minutes > 0) sb.append("${minutes}分")
        if (seconds > 0) sb.append("${seconds}秒")
        if (millSec > 0) sb.append("${millSec}毫秒")
    }
    return sb.toString()
}


