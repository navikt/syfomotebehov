package no.nav.syfo.motebehov.api.internad.dto

import no.nav.syfo.motebehov.MotebehovFormValuesOutputDTO
import no.nav.syfo.motebehov.MotebehovSvarLegacyDTO
import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType
import java.time.LocalDateTime
import java.util.*

data class MotebehovVeilederDTOv3(
    val id: UUID,
    val opprettetDato: LocalDateTime,
    val aktorId: String,
    val opprettetAv: String,
    val opprettetAvNavn: String?,
    val arbeidstakerFnr: String,
    val virksomhetsnummer: String,
    val motebehovSvar: MotebehovSvarLegacyDTO,
    val tildeltEnhet: String? = null,
    val behandletTidspunkt: LocalDateTime? = null,
    val behandletVeilederIdent: String? = null,
    val skjemaType: MotebehovSkjemaType? = null
)

data class MotebehovVeilederDTOv4(
    val id: UUID,
    val opprettetDato: LocalDateTime,
    val opprettetAv: String,
    val opprettetAvNavn: String?,
    val arbeidstakerFnr: String,
    val virksomhetsnummer: String,
    val behandletTidspunkt: LocalDateTime? = null,
    val behandletVeilederIdent: String? = null,
//    val innmelderType: InnmelderType, // ARBEIDSTAKER | ARBEIDSGIVER,
    val skjemaType: MotebehovSkjemaType? = null, // make not nullable
    val formValues: MotebehovFormValuesOutputDTO,
)
