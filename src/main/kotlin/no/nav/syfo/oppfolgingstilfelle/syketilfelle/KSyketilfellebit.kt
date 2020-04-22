package no.nav.syfo.oppfolgingstilfelle.syketilfelle

import java.time.LocalDateTime

data class KSyketilfellebit(
    val id: String? = null,
    val aktorId: String,
    val orgnummer: String? = null,
    val opprettet: LocalDateTime,
    val inntruffet: LocalDateTime,
    val tags: List<String>,
    val ressursId: String,
    val fom: LocalDateTime,
    val tom: LocalDateTime
)
