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

fun PPersonOppfolgingstilfelle.mapToPersonOppfolgingstilfelle(): PersonOppfolgingstilfelle {
    return PersonOppfolgingstilfelle(
        fnr = this.fnr,
        fom = this.fom,
        tom = this.tom
    )
}

fun PPersonOppfolgingstilfelle.mapToPersonVirksomhetOppfolgingstilfelle(): PersonVirksomhetOppfolgingstilfelle {
    return PersonVirksomhetOppfolgingstilfelle(
        fnr = this.fnr,
        virksomhetsnummer = this.virksomhetsnummer,
        fom = this.fom,
        tom = this.tom
    )
}

fun PPersonOppfolgingstilfelle.isDateInOppfolgingstilfelle(date: LocalDate): Boolean {
    return date.isAfter(this.fom.minusDays(1)) && date.isBefore(this.tom.plusDays(1))
}
fun PPersonOppfolgingstilfelle.isActiveLast16Days(): Boolean {
    return tom.isAfter(LocalDate.now().minusDays(16)) && fom.isBefore(LocalDate.now().plusDays(1))
}
