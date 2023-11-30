package no.nav.syfo.motebehov.api.internad.dto

import no.nav.syfo.motebehov.motebehovstatus.*
import java.io.Serializable
import java.time.LocalDateTime
import java.util.*

data class MotebehovVeilederDTO(
    val id: UUID,
    val opprettetDato: LocalDateTime,
    val aktorId: String,
    val opprettetAv: String,
    val opprettetAvNavn: String?,
    val arbeidstakerFnr: String,
    val virksomhetsnummer: String,
    val motebehovSvar: MotebehovSvarVeilederDTO,
    val tildeltEnhet: String? = null,
    val behandletTidspunkt: LocalDateTime? = null,
    val behandletVeilederIdent: String? = null,
    val skjemaType: MotebehovSkjemaType? = null
) : Serializable
