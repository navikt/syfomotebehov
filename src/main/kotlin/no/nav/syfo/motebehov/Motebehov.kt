package no.nav.syfo.motebehov

import no.nav.syfo.motebehov.api.internad.dto.MotebehovVeilederDTO
import no.nav.syfo.motebehov.database.PMotebehov
import no.nav.syfo.motebehov.motebehovstatus.DAYS_END_SVAR_BEHOV
import no.nav.syfo.motebehov.motebehovstatus.DAYS_START_SVAR_BEHOV
import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType
import no.nav.syfo.oppfolgingstilfelle.database.PersonOppfolgingstilfelle
import java.io.Serializable
import java.time.LocalDateTime
import java.util.*

data class Motebehov(
    val id: UUID,
    val opprettetDato: LocalDateTime,
    val aktorId: String,
    val opprettetAv: String,
    val opprettetAvFnr: String,
    val arbeidstakerFnr: String,
    val virksomhetsnummer: String,
    val formSubmission: MotebehovFormSubmissionCombinedDTO,
    val tildeltEnhet: String? = null,
    val behandletTidspunkt: LocalDateTime? = null,
    val behandletVeilederIdent: String? = null,
    val skjemaType: MotebehovSkjemaType? = null,
) : Serializable

data class MotebehovOutputDTO(
    val id: UUID,
    val opprettetDato: LocalDateTime,
    val aktorId: String,
    val opprettetAv: String,
    val opprettetAvFnr: String,
    val arbeidstakerFnr: String,
    val virksomhetsnummer: String,
    val formValues: MotebehovFormValuesOutputDTO,
    val tildeltEnhet: String? = null,
    val behandletTidspunkt: LocalDateTime? = null,
    val behandletVeilederIdent: String? = null,
    val skjemaType: MotebehovSkjemaType? = null,
)

fun List<Motebehov>.toMotebehovVeilederDTOList() =
    this.map { it.toMotebehovVeilederDTO() }

fun Motebehov.toMotebehovVeilederDTO(): MotebehovVeilederDTO {
    return MotebehovVeilederDTO(
        id = this.id,
        opprettetDato = this.opprettetDato,
        aktorId = this.aktorId,
        opprettetAv = this.opprettetAv,
        opprettetAvNavn = null,
        arbeidstakerFnr = this.arbeidstakerFnr,
        virksomhetsnummer = this.virksomhetsnummer,
        motebehovSvar = this.formSubmission.toMotebehovFormValuesOutputDTO(),
        tildeltEnhet = this.tildeltEnhet,
        behandletTidspunkt = this.behandletTidspunkt,
        behandletVeilederIdent = this.behandletVeilederIdent, skjemaType = this.skjemaType,
    )
}

fun Motebehov.isUbehandlet(): Boolean {
    return this.formSubmission.harMotebehov && this.behandletVeilederIdent.isNullOrEmpty()
}

fun Motebehov.isCreatedInOppfolgingstilfelle(oppfolgingstilfelle: PersonOppfolgingstilfelle): Boolean {
    val createdDate = this.opprettetDato.toLocalDate()
    return createdDate.isAfter(oppfolgingstilfelle.fom.minusDays(1)) &&
        createdDate.isBefore(oppfolgingstilfelle.tom.plusDays(17))
}

fun Motebehov.isSvarBehovForOppfolgingstilfelle(oppfolgingstilfelle: PersonOppfolgingstilfelle): Boolean {
    val firstDateSvarBehovAvailability = oppfolgingstilfelle.fom.plusDays(DAYS_START_SVAR_BEHOV)
    val lastDateSvarBehovAvailability = oppfolgingstilfelle.fom.plusDays(DAYS_END_SVAR_BEHOV).minusDays(1)
    val createdDate = this.opprettetDato.toLocalDate()
    return createdDate.isAfter(firstDateSvarBehovAvailability.minusDays(1)) &&
        createdDate.isBefore(lastDateSvarBehovAvailability.plusDays(1))
}

fun Motebehov.toPMotebehov(): PMotebehov =
    PMotebehov(
        uuid = this.id,
        opprettetDato = this.opprettetDato,
        opprettetAv = this.opprettetAv,
        aktoerId = this.aktorId,
        virksomhetsnummer = this.virksomhetsnummer,
        harMotebehov = this.formSubmission.harMotebehov,
        forklaring = this.formSubmission.forklaring,
        tildeltEnhet = this.tildeltEnhet,
        behandletVeilederIdent = this.behandletVeilederIdent,
        behandletTidspunkt = this.behandletTidspunkt,
        skjemaType = this.skjemaType,
        sykmeldtFnr = this.arbeidstakerFnr,
        opprettetAvFnr = this.opprettetAvFnr,
        formSnapshot = this.formSubmission.formSnapshot,
    )

fun Motebehov.toMotebehovOutputDTO(): MotebehovOutputDTO =
    MotebehovOutputDTO(
        id = this.id,
        opprettetDato = this.opprettetDato,
        aktorId = this.aktorId,
        opprettetAv = this.opprettetAv,
        opprettetAvFnr = this.opprettetAvFnr,
        arbeidstakerFnr = this.arbeidstakerFnr,
        virksomhetsnummer = this.virksomhetsnummer,
        tildeltEnhet = this.tildeltEnhet,
        behandletTidspunkt = this.behandletTidspunkt,
        behandletVeilederIdent = this.behandletVeilederIdent,
        skjemaType = this.skjemaType,
        formValues = this.formSubmission.toMotebehovFormValuesOutputDTO(),
    )
