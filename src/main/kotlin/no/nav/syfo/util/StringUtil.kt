package no.nav.syfo.util

import java.util.*

fun String.lowerCapitalize() =
    this.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

fun String.mapToBoolean(): Boolean {
    return this == "1"
}

fun Boolean.mapToString(): String {
    return if (this) "1" else "0"
}
