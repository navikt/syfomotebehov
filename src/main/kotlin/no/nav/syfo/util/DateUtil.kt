package no.nav.syfo.util

import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime

fun convert(localDateTime: LocalDateTime): Timestamp =
    Timestamp.valueOf(localDateTime)

fun convertNullable(localDateTime: LocalDateTime?): Timestamp? {
    return if (localDateTime != null) {
        Timestamp.valueOf(localDateTime)
    } else {
        null
    }
}

fun convert(localDate: LocalDate): Timestamp =
    Timestamp.valueOf(localDate.atStartOfDay())

fun convertNullable(timestamp: Timestamp?): LocalDateTime? =
    timestamp?.toLocalDateTime()

fun convert(timestamp: Timestamp): LocalDate =
    timestamp.toLocalDateTime().toLocalDate()
