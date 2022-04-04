package no.nav.syfo.varsel.dinesykmeldte.domain

import java.time.OffsetDateTime

data class DineSykmeldteHendelse(
    val id: String,
    val opprettHendelse: OpprettHendelse?,
    val ferdigstillHendelse: FerdigstillHendelse?
)

data class OpprettHendelse(
    val ansattFnr: String,
    val orgnummer: String,
    val oppgavetype: String,
    val lenke: String?,
    val tekst: String?,
    val timestamp: OffsetDateTime,
    val utlopstidspunkt: OffsetDateTime?
)

data class FerdigstillHendelse(
    val timestamp: OffsetDateTime
)
