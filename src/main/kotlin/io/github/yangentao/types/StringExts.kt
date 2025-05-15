package io.github.yangentao.types

operator fun StringBuilder.plusAssign(s: String) {
    this.append(s)
}

operator fun StringBuilder.plusAssign(ch: Char) {
    this.append(ch)
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


