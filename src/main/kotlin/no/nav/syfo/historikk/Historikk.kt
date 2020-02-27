package no.nav.syfo.historikk

import java.time.LocalDateTime

data class Historikk(
        var opprettetAv: String? = null,
        var tekst: String,
        var tidspunkt: LocalDateTime
)
