package no.nav.syfo.oppfolgingstilfelle.database

import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import java.time.LocalDate

data class PersonOppfolgingstilfelle(
    val fnr: Fodselsnummer,
    val fom: LocalDate,
    val tom: LocalDate
)
