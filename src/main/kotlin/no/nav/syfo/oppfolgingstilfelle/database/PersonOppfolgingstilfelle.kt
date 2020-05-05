package no.nav.syfo.oppfolgingstilfelle.database

import java.time.LocalDate

data class PersonOppfolgingstilfelle(
        val fnr: String,
        val virksomhetsnummer: String,
        val fom: LocalDate,
        val tom: LocalDate
)
