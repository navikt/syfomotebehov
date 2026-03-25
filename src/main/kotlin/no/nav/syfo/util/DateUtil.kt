package no.nav.syfo.util

import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

val ZONE_ID = "Europe/Oslo"

fun convert(localDateTime: LocalDateTime): Timestamp = Timestamp.valueOf(localDateTime)

fun convertNullable(localDateTime: LocalDateTime?): Timestamp? =
    if (localDateTime != null) {
        Timestamp.valueOf(localDateTime)
    } else {
        null
    }

fun convert(localDate: LocalDate): Timestamp = Timestamp.valueOf(localDate.atStartOfDay())

fun convertNullable(timestamp: Timestamp?): LocalDateTime? = timestamp?.toLocalDateTime()

fun convert(timestamp: Timestamp): LocalDate = timestamp.toLocalDateTime().toLocalDate()

fun convertInstantToLocalDateTime(instant: Instant): LocalDateTime = LocalDateTime.ofInstant(instant, ZoneId.of(ZONE_ID))

fun convertLocalDateTimeToInstant(localDateTime: LocalDateTime): Instant = localDateTime.atZone(ZoneId.of(ZONE_ID)).toInstant()

fun OffsetDateTime.toNorwegianLocalDateTime(): LocalDateTime =
    this
        .atZoneSameInstant(ZoneId.of("Europe/Oslo"))
        .toLocalDateTime()

fun LocalDateTime.isEqualOrAfter(other: LocalDateTime): Boolean = this.isEqual(other) || this.isAfter(other)
