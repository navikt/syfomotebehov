package no.nav.syfo.oppfolgingstilfelle.database

import java.time.LocalDate

data class PersonOppfolgingstilfelle(
    val fnr: String,
    val fom: LocalDate,
    val tom: LocalDate
)

fun PersonOppfolgingstilfelle.isDateInOppfolgingstilfelle(date: LocalDate): Boolean {
    return date.isAfter(this.fom.minusDays(1)) && date.isBefore(this.tom.plusDays(1))
}
