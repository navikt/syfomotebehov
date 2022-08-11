package no.nav.syfo.extensions

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

fun OffsetDateTime.toNorwegianLocalDateTime(): LocalDateTime = this.atZoneSameInstant(ZoneId.of("Europe/Oslo"))
    .toLocalDateTime()
