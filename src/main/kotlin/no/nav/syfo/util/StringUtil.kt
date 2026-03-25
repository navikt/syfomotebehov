package no.nav.syfo.util

import java.util.Locale

fun String.lowerCapitalize() =
    this.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
