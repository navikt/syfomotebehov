package no.nav.syfo.oppfolgingstilfelle.database

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class PPersonOppfolgingstilfelle(
        val uuid: UUID,
        val opprettet: LocalDateTime,
        val sistEndret: LocalDateTime,
        val fnr: String,
        val virksomhetsnummer: String,
        val fom: LocalDate,
        val tom: LocalDate
)
