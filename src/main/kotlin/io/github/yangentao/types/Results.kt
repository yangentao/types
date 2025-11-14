package io.github.yangentao.types

class ItemsResult<T> private constructor(val items: List<T> = emptyList(), val offset: Int? = null, val total: Int? = null, rawValue: Any? = null, error: CommonError? = null) : CommonResult(rawValue, error) {

    val size: Int get() = items.size
    val isEmpty: Boolean get() = items.isEmpty()
    val isNoEmpty: Boolean get() = items.isNotEmpty()

    fun get(index: Int): T = items[index]
    fun getOr(index: Int): T? = items.getOrNull(index)

    companion object {
        fun <T> success(items: List<T>, offset: Int? = null, total: Int? = null, rawValue: Any? = null): ItemsResult<T> {
            return ItemsResult<T>(items = items, offset = offset, total = total, rawValue = rawValue)
        }

        fun <T> failed(error: CommonError?): ItemsResult<T> {
            return ItemsResult<T>(error = error)
        }

        fun <T> failed(message: String, code: Int = -1, data: Any? = null): ItemsResult<T> {
            return ItemsResult<T>(error = CommonError(message, code, data))
        }
    }
}

class MapResult<T> private constructor(val map: Map<String, T> = emptyMap(), rawValue: Any? = null, error: CommonError? = null) : CommonResult(rawValue, error) {

    val size: Int get() = map.size
    val isEmpty: Boolean get() = map.isEmpty()
    val isNoEmpty: Boolean get() = map.isNotEmpty()

    val keys: Set<String> get() = map.keys
    fun containsKey(key: String): Boolean = map.containsKey(key)
    fun get(key: String): T? = map[key]

    companion object {
        fun <T> success(map: Map<String, T>, rawValue: Any? = null): MapResult<T> {
            return MapResult<T>(map = map, rawValue = rawValue)
        }

        fun <T> failed(error: CommonError?): MapResult<T> {
            return MapResult<T>(error = error)
        }

        fun <T> failed(message: String, code: Int = -1, data: Any? = null): MapResult<T> {
            return MapResult<T>(error = CommonError(message, code, data))
        }
    }
}

class SingleResult<T> private constructor(private val _value: T? = null, rawValue: Any? = null, error: CommonError? = null) : CommonResult(rawValue, error) {

    @Suppress("UNCHECKED_CAST")
    val value: T get() = _value as T

    companion object {
        fun <T> success(value: T, rawValue: Any? = null): SingleResult<T> {
            return SingleResult<T>(_value = value, rawValue = rawValue)
        }

        fun <T> failed(error: CommonError?): SingleResult<T> {
            return SingleResult<T>(error = error)
        }

        fun <T> failed(message: String, code: Int = -1, data: Any? = null): SingleResult<T> {
            return SingleResult<T>(error = CommonError(message, code, data))
        }
    }

}

open class CommonResult(val rawValue: Any? = null, val error: CommonError? = null) {
    val success: Boolean = error == null
    val failed: Boolean = error != null
    val code: Int = error!!.code
    val message: String = error!!.message
}

class CommonError(val message: String, val code: Int = -1, val data: Any? = null)