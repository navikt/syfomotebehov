package no.nav.syfo.motebehov.database

import no.nav.syfo.motebehov.CreateFormSnapshotFromLegacyMotebehovHelper
import no.nav.syfo.motebehov.Motebehov
import no.nav.syfo.motebehov.MotebehovFormValues
import no.nav.syfo.motebehov.MotebehovInnmelderType
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
    val opprettetAvFnr: String? = null,
    // For old "legacy motebehov", this field will be null. For new motebehov that was submitted with a formSnapshot,
    // this field will be populated with values from the corresponding MOTEBEHOV_FORM_VALUES table.
    val motebehovFormValues: PMotebehovFormValues? = null,
) : Serializable

data class PMotebehovFormValues(
    val formSnapshotJSON: String?,
    // These values for these fields are extracted from the formSnapshot
    val begrunnelse: String?,
    val onskerSykmelderDeltar: Boolean,
    val onskerSykmelderDeltarBegrunnelse: String?,
    val onskerTolk: Boolean,
    val tolkSprak: String?,
)

fun PMotebehov.toMotebehov(
    arbeidstakerFnr: String? = null,
    knownInnmelderType: MotebehovInnmelderType? = null
): Motebehov {
    return Motebehov(
        id = this.uuid,
        opprettetDato = this.opprettetDato,
        aktorId = this.aktoerId,
        opprettetAv = this.opprettetAv,
        opprettetAvFnr = this.opprettetAvFnr!!,
        arbeidstakerFnr = arbeidstakerFnr ?: this.sykmeldtFnr!!,
        virksomhetsnummer = this.virksomhetsnummer,
        formValues = createMotebehovSvarFromPMotebehov(this, knownInnmelderType),
        tildeltEnhet = this.tildeltEnhet,
        behandletTidspunkt = this.behandletTidspunkt,
        behandletVeilederIdent = this.behandletVeilederIdent,
        skjemaType = this.skjemaType,
    )
}

private fun createMotebehovSvarFromPMotebehov(
    pMotebehov: PMotebehov,
    knownInnmelderType: MotebehovInnmelderType?
): MotebehovFormValues {
    val motebehovInnmelderType = knownInnmelderType
        ?: if (pMotebehov.opprettetAv == pMotebehov.aktoerId ||
            pMotebehov.opprettetAvFnr == pMotebehov.sykmeldtFnr
        ) {
            MotebehovInnmelderType.ARBEIDSTAKER
        } else {
            MotebehovInnmelderType.ARBEIDSGIVER
        }

    val isLegacyMotebehov = pMotebehov.motebehovFormValues == null

    // Legacy pMotebehov db entries will not have formValues containing a formSnapshot. In that case we create
    // a formSnapshot representing the legacy motebehov from harMotebehov and forklaring.
    val formSnapshot = if (isLegacyMotebehov) {
        val helper = CreateFormSnapshotFromLegacyMotebehovHelper()
        helper.createFormSnapshotFromLegacyMotebehov(
            pMotebehov.harMotebehov,
            pMotebehov.forklaring,
            pMotebehov.skjemaType,
            motebehovInnmelderType,
        )
    } else {
        pMotebehov.motebehovFormValues?.formSnapshotJSON?.let { convertJsonToFormSnapshot(it) }
    }

    return MotebehovFormValues(
        harMotebehov = pMotebehov.harMotebehov,
        forklaring = pMotebehov.forklaring,
        formSnapshot = formSnapshot
    )
}
