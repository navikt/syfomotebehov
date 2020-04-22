package no.nav.syfo.util

import java.sql.Timestamp
import java.time.LocalDate

fun convert(localDate: LocalDate): Timestamp =
        Timestamp.valueOf(localDate.atStartOfDay())

fun convert(timestamp: Timestamp): LocalDate =
        timestamp.toLocalDateTime().toLocalDate()
