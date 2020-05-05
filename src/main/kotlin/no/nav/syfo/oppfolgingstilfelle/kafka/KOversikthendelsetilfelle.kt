package no.nav.syfo.oppfolgingstilfelle.kafka

import java.time.LocalDate
import java.time.LocalDateTime

data class KOversikthendelsetilfelle(
        val fnr: String,
        val navn: String,
        val enhetId: String,
        val virksomhetsnummer: String,
        val virksomhetsnavn: String,
        val gradert: Boolean,
        val fom: LocalDate,
        val tom: LocalDate,
        val tidspunkt: LocalDateTime
)
