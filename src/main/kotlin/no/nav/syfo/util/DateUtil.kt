package no.nav.syfo.util

import java.sql.Timestamp
import java.time.*

val ZONE_ID = "Europe/Oslo"

fun convert(localDateTime: LocalDateTime): Timestamp =
    Timestamp.valueOf(localDateTime)

fun convertNullable(localDateTime: LocalDateTime?): Timestamp? {
    return if (localDateTime != null) {
        Timestamp.valueOf(localDateTime)
    } else {
        null
    }
}

fun convert(localDate: LocalDate): Timestamp = Timestamp.valueOf(localDate.atStartOfDay())

fun convertNullable(timestamp: Timestamp?): LocalDateTime? = timestamp?.toLocalDateTime()

fun convert(timestamp: Timestamp): LocalDate = timestamp.toLocalDateTime().toLocalDate()

fun convertInstantToLocalDateTime(instant: Instant): LocalDateTime {
    return LocalDateTime.ofInstant(instant, ZoneId.of(ZONE_ID))
}

fun convertLocalDateTimeToInstant(localDateTime: LocalDateTime): Instant {
    return localDateTime.atZone(ZoneId.of(ZONE_ID)).toInstant()
}
