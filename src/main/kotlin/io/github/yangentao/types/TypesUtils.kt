package io.github.yangentao.types

import java.util.UUID

fun UUID.formatText(): String {
    return UUID.randomUUID().toString().replace("-", "")
}