package io.github.yangentao.types

operator fun StringBuilder.plusAssign(s: String) {
    this.append(s)
}

operator fun StringBuilder.plusAssign(ch: Char) {
    this.append(ch)
}

infix fun <T : Appendable> T.add(s: CharSequence?): T {
    if (s != null) this.append(s)
    return this
}

infix fun <T : Appendable> T.add(ch: Char): T {
    this.append(ch)
    return this
}

//ignore case equal
infix fun String?.ieq(other: String?): Boolean {
    return this.equals(other, ignoreCase = true)
}

val String.quoted: String get() = if (this.startsWith("\"") && this.endsWith("\"")) this else "\"$this\"";

// a => 'a'
val String.quotedSingle: String
    get() {
        if (this.startsWith("'") && this.endsWith("'")) return this
        return "'$this'"
    }

infix fun String?.or(other: String): String {
    if (this.isNullOrEmpty()) return other
    return this
}

fun String.substr(from: Int, size: Int): String {
    val a = if (from >= 0) {
        from
    } else 0
    val b = if (a + size < this.length) {
        a + size
    } else {
        this.length
    }
    return this.substring(a, b)
}

object StringComparatorIgnoreCase : Comparator<String> {
    override fun compare(o1: String, o2: String): Int {
        return o1.compareTo(o2, ignoreCase = true)
    }
}

fun CharSequence?.empty(): Boolean {
    return this.isNullOrEmpty()
}

fun String.replaceChars(vararg charValuePair: Pair<Char, String>): String {
    val sb = StringBuilder(this.length + 8)
    for (ch in this) {
        val p = charValuePair.find { it.first == ch }
        if (p != null) {
            sb.append(p.second)
        } else {
            sb.append(ch)
        }
    }
    return sb.toString()
}

//"abcd=defg-123".substringBetween('=','-') => "defg"
//"abcd=defg=123".substringBetween('=','=') => "defg"
//"abcd==123".substringBetween('=','=') => ""
//"abcd=123".substringBetween('=','=') => null
fun String.substringBetween(a: Char, b: Char): String? {
    val nA = this.indexOf(a)
    if (nA >= 0) {
        val nB = this.indexOf(b, nA + 1)
        if (nB >= 0) {
            return this.substring(nA + 1, nB)
        }
    }
    return null
}

//left: 左边界, 如果是空字符串或null, 表示开始位置
//right: 右边界, 如果是空字符串或null, 表示结束位置
fun String.substringBetween(left: String?, right: String?, startIndex: Int = 0, ignoreCase: Boolean = false): String? {
    if (left.isNullOrEmpty()) {
        if (right.isNullOrEmpty()) {
            return this
        } else {
            val n = this.indexOf(right, startIndex, ignoreCase)
            if (n >= 0) return this.substring(0, n)
            return null
        }
    } else {
        if (right.isNullOrEmpty()) {
            val n = this.indexOf(left, startIndex, ignoreCase)
            if (n >= 0) return this.substring(n + left.length)
            return null
        }
    }

    val nA = this.indexOf(left, startIndex, ignoreCase)
    if (nA >= 0) {
        val nB = this.indexOf(right, nA + left.length, ignoreCase)
        if (nB >= 0) {
            return this.substring(nA + left.length, nB)
        }
    }
    return null
}

fun String.escapeHtml(): String {
    val sb = StringBuffer((this.length * 1.1).toInt())
    this.forEach {
        when (it) {
            '<' -> sb.append("&lt;")
            '>' -> sb.append("&gt;")
            '"' -> sb.append("&quot;")
            '\'' -> sb.append("&#x27;")
            '&' -> sb.append("&amp;")
            '/' -> sb.append("&#x2F;")
            else -> sb.append(it)
        }
    }
    return sb.toString()
}


