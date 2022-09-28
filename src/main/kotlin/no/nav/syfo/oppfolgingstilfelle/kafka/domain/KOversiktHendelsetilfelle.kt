package no.nav.syfo.oppfolgingstilfelle.kafka.domain

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

fun KOversikthendelsetilfelle.previouslyProcessed(
    lastUpdatedAt: LocalDateTime?
) = lastUpdatedAt?.let { it ->
    this.tidspunkt.isBefore(it)
} ?: false
