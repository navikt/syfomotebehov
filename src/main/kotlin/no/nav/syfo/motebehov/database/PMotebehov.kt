package no.nav.syfo.motebehov.database

import no.nav.syfo.motebehov.Motebehov
import no.nav.syfo.motebehov.MotebehovFormSubmissionCombinedDTO
import no.nav.syfo.motebehov.MotebehovInnmelderType
import no.nav.syfo.motebehov.formSnapshot.FormSnapshot
import no.nav.syfo.motebehov.formSnapshot.LegacyMotebehovToFormSnapshotHelper
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
    val skjemaType: MotebehovSkjemaType,
    val innmelderType: MotebehovInnmelderType,
    val sykmeldtFnr: String? = null,
    val opprettetAvFnr: String? = null,
    // For old "legacy motebehov", this field will be null.
    val formSnapshot: FormSnapshot? = null,
) : Serializable

fun PMotebehov.toMotebehov(
    arbeidstakerFnr: String? = null,
): Motebehov {
    return Motebehov(
        id = this.uuid,
        opprettetDato = this.opprettetDato,
        aktorId = this.aktoerId,
        opprettetAv = this.opprettetAv,
        opprettetAvFnr = this.opprettetAvFnr!!,
        arbeidstakerFnr = arbeidstakerFnr ?: this.sykmeldtFnr!!,
        virksomhetsnummer = this.virksomhetsnummer,
        tildeltEnhet = this.tildeltEnhet,
        behandletTidspunkt = this.behandletTidspunkt,
        behandletVeilederIdent = this.behandletVeilederIdent,
        skjemaType = this.skjemaType,
        innmelderType = this.innmelderType,
        formSubmission = createMotebehovFormSubmissionFromPMotebehov(this, this.innmelderType),
    )
}

private fun createMotebehovFormSubmissionFromPMotebehov(
    pMotebehov: PMotebehov,
    innmelderType: MotebehovInnmelderType,
): MotebehovFormSubmissionCombinedDTO {
    val isLegacyMotebehov = pMotebehov.formSnapshot == null

    val formSnapshot = if (isLegacyMotebehov) {
        val helper = LegacyMotebehovToFormSnapshotHelper()
        helper.createFormSnapshotFromLegacyMotebehovValues(
            pMotebehov.harMotebehov,
            pMotebehov.forklaring,
            pMotebehov.skjemaType,
            innmelderType,
        )
    } else {
        pMotebehov.formSnapshot
    }

    return MotebehovFormSubmissionCombinedDTO(
        harMotebehov = pMotebehov.harMotebehov,
        forklaring = pMotebehov.forklaring,
        formSnapshot = formSnapshot
    )
}
