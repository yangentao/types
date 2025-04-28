package io.github.yangentao.types


object Hex {
    private val DICT = "0123456789ABCDEF"

    // 8->08,  17->1f
    fun encode(b: Int): String {
        val arr = CharArray(2)
        arr[0] = DICT[0x0f and (b ushr 4)]
        arr[1] = DICT[0x0f and b]
        return String(arr)
    }

    fun encode(bytes: ByteArray): String {
        val ret = StringBuilder(2 * bytes.size)
        for (a in bytes) {
            val n = a.toInt()
            ret.append(DICT[0x0f and (n ushr 4)])
            ret.append(DICT[0x0f and n])
        }
        return ret.toString()
    }

    fun decode(hexString: String?): ByteArray? {
        if (hexString == null) {
            return null
        }
        val strLen = hexString.length
        if (strLen == 0) {
            return ByteArray(0)
        }
        if (strLen % 2 != 0) {
            throw IllegalArgumentException("字符串长度必须是2的倍数")
        }
        val s = hexString.uppercase()

        val bytes = ByteArray(strLen / 2)
        var i = 0
        while (i < strLen) {
            val hi = toByte(s[i])
            val lo = toByte(s[i + 1])
            val n = ((hi shl 4) and 0xf0) or (lo and 0x0f)
            bytes[i / 2] = n.toByte()
            i += 2
        }
        return bytes
    }

    fun toByte(ch: Char): Int {
        if (ch in '0'..'9') {
            return ch - '0'
        }
        if (ch in 'A'..'F') {
            return ch - 'A' + 10
        }
        if (ch in 'a'..'f') {
            return ch - 'a' + 10
        }
        throw IllegalArgumentException("不合法的字符$ch")
    }
}