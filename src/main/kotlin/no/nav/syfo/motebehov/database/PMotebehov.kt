package no.nav.syfo.motebehov.database

import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType
import java.io.Serializable
import java.time.LocalDateTime
import java.util.*

data class PMotebehov(
    val uuid: UUID,
    val opprettetDato: LocalDateTime,
    val opprettetAv: String,
    val aktoerId: String,
    val virksomhetsnummer: String,
    val harMotebehov: Boolean,
    val forklaring: String? = null,
    val tildeltEnhet: String? = null,
    val behandletTidspunkt: LocalDateTime? = null,
    val behandletVeilederIdent: String? = null,
    val skjemaType: MotebehovSkjemaType? = null,
    val sykmeldtFnr: String? = null,
    val opprettetAvFnr: String? = null
) : Serializable
