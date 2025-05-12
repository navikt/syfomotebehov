package no.nav.syfo.motebehov

import no.nav.syfo.motebehov.api.internad.dto.MotebehovVeilederDTOv3
import no.nav.syfo.motebehov.api.internad.dto.MotebehovVeilederDTOv4
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
    val tildeltEnhet: String? = null,
    val behandletTidspunkt: LocalDateTime? = null,
    val behandletVeilederIdent: String? = null,
    val skjemaType: MotebehovSkjemaType,
    val innmelderType: MotebehovInnmelderType? = null,
    val formSubmission: MotebehovFormSubmissionCombinedDTO,
) : Serializable

data class MotebehovWithLegacyMotebehovSvarOutputDTO(
    val id: UUID,
    val opprettetDato: LocalDateTime,
    val aktorId: String,
    val opprettetAv: String,
    val opprettetAvFnr: String,
    val arbeidstakerFnr: String,
    val virksomhetsnummer: String,
    val tildeltEnhet: String? = null,
    val behandletTidspunkt: LocalDateTime? = null,
    val behandletVeilederIdent: String? = null,
    val skjemaType: MotebehovSkjemaType,
    val motebehovSvar: MotebehovSvarLegacyDTO,
)

data class MotebehovWithFormValuesOutputDTO(
    val id: UUID,
    val opprettetDato: LocalDateTime,
    val aktorId: String,
    val opprettetAv: String,
    val opprettetAvFnr: String,
    val arbeidstakerFnr: String,
    val virksomhetsnummer: String,
    val tildeltEnhet: String? = null,
    val behandletTidspunkt: LocalDateTime? = null,
    val behandletVeilederIdent: String? = null,
    val skjemaType: MotebehovSkjemaType,
    val innmelderType: MotebehovInnmelderType?,
    val formValues: MotebehovFormValuesOutputDTO,
)

fun List<Motebehov>.toMotebehovVeilederDTOList() =
    this.map { it.toMotebehovVeilederDTO() }

fun Motebehov.toMotebehovVeilederDTO(): MotebehovVeilederDTOv3 {
    return MotebehovVeilederDTOv3(
        id = this.id,
        opprettetDato = this.opprettetDato,
        aktorId = this.aktorId,
        opprettetAv = this.opprettetAv,
        opprettetAvNavn = null,
        arbeidstakerFnr = this.arbeidstakerFnr,
        virksomhetsnummer = this.virksomhetsnummer,
        motebehovSvar = MotebehovSvarLegacyDTO(this.formSubmission.harMotebehov, this.formSubmission.forklaring),
        tildeltEnhet = this.tildeltEnhet,
        behandletTidspunkt = this.behandletTidspunkt,
        behandletVeilederIdent = this.behandletVeilederIdent,
        skjemaType = this.skjemaType,
    )
}

fun List<Motebehov>.toMotebehovVeilederDTOv4List() =
    this.map { it.toMotebehovVeilederDTOv4() }

fun Motebehov.toMotebehovVeilederDTOv4(): MotebehovVeilederDTOv4 {
    return MotebehovVeilederDTOv4(
        id = this.id,
        opprettetDato = this.opprettetDato,
        opprettetAv = this.opprettetAv,
        opprettetAvNavn = null,
        arbeidstakerFnr = this.arbeidstakerFnr,
        virksomhetsnummer = this.virksomhetsnummer,
        behandletTidspunkt = this.behandletTidspunkt,
        behandletVeilederIdent = this.behandletVeilederIdent,
        skjemaType = this.skjemaType,
        innmelderType = this.innmelderType,
        formValues = this.formSubmission.toMotebehovFormValuesOutputDTO(),
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
        innmelderType = this.innmelderType,
        sykmeldtFnr = this.arbeidstakerFnr,
        opprettetAvFnr = this.opprettetAvFnr,
        formSnapshot = this.formSubmission.formSnapshot,
    )

fun Motebehov.toMotebehovWithFormValuesOutputDTO(): MotebehovWithFormValuesOutputDTO =
    MotebehovWithFormValuesOutputDTO(
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
        innmelderType = this.innmelderType,
        formValues = this.formSubmission.toMotebehovFormValuesOutputDTO(),
    )

fun Motebehov.toMotebehovWithLegacyMotebehovSvarOutputDTO(): MotebehovWithLegacyMotebehovSvarOutputDTO =
    MotebehovWithLegacyMotebehovSvarOutputDTO(
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
        motebehovSvar = this.formSubmission.toMotebehovSvarLegacyDTO()
    )
