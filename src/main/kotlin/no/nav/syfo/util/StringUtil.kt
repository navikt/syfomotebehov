package no.nav.syfo.util

import java.util.*

fun String.lowerCapitalize() =
    this.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
