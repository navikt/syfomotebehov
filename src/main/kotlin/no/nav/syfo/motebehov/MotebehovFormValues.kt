package no.nav.syfo.motebehov

import no.nav.syfo.motebehov.database.PMotebehovFormValues
import no.nav.syfo.motebehov.database.convertFormSnapshotToJson
import no.nav.syfo.motebehov.formSnapshot.BEGRUNNELSE_TEXT_FIELD_ID
import no.nav.syfo.motebehov.formSnapshot.FormSnapshot
import no.nav.syfo.motebehov.formSnapshot.ONSKER_SYKMELDER_DELTAR_BEGRUNNELSE_TEXT_FIELD_ID
import no.nav.syfo.motebehov.formSnapshot.ONSKER_SYKMELDER_DELTAR_CHECKBOX_FIELD_ID
import no.nav.syfo.motebehov.formSnapshot.ONSKER_TOLK_CHECKBOX_FIELD_ID
import no.nav.syfo.motebehov.formSnapshot.TOLK_SPRAK_TEXT_FIELD_ID
import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType

data class MotebehovFormValues(
    val harMotebehov: Boolean,
    // This forklaring field is to be phased in favor of formSnapshot, and eventually removed.
    val forklaring: String? = null,
    val formSnapshot: FormSnapshot?
)

// Existing input DTO to phase out. MotebehovFormValuesInputDTO will take over.
data class MotebehovSvarInputDTO(
    val harMotebehov: Boolean,
    val forklaring: String? = null,
)

data class MotebehovFormValuesInputDTO(
    val harMotebehov: Boolean,
    val formSnapshot: FormSnapshot,
    // New fields to get on input and store, instead of having to calculate them in a probably unstable way.
    val skjemaType: MotebehovSkjemaType,
    val innmelderType: MotebehovInnmelderType,
)

data class TemporaryCombinedNyttMotebehovSvar(
    val harMotebehov: Boolean,
    val forklaring: String? = null,
    val formSnapshot: FormSnapshot?,
)

data class MotebehovFormValuesOutputDTO(
    val harMotebehov: Boolean,
    val forklaring: String? = null,
    val formSnapshot: FormSnapshot?,
    val begrunnelse: String? = null,
    val onskerSykmelderDeltar: Boolean? = null,
    val onskerSykmelderDeltarBegrunnelse: String? = null,
    val onskerTolk: Boolean? = null,
    val tolkSprak: String? = null,
)

data class MotebehovFormValuesFromFormSnapshot(
    val begrunnelse: String? = null,
    val onskerSykmelderDeltar: Boolean,
    val onskerSykmelderDeltarBegrunnelse: String? = null,
    val onskerTolk: Boolean,
    val tolkSprak: String? = null,
)

fun extractValuesFromFormSnapshot(formSnapshot: FormSnapshot): MotebehovFormValuesFromFormSnapshot {
    val fieldValues = formSnapshot.fieldValues

    return MotebehovFormValuesFromFormSnapshot(
        begrunnelse = fieldValues[BEGRUNNELSE_TEXT_FIELD_ID] as? String,
        onskerSykmelderDeltar = fieldValues[ONSKER_SYKMELDER_DELTAR_CHECKBOX_FIELD_ID] as? Boolean ?: false,
        onskerSykmelderDeltarBegrunnelse = fieldValues[ONSKER_SYKMELDER_DELTAR_BEGRUNNELSE_TEXT_FIELD_ID] as? String,
        onskerTolk = fieldValues[ONSKER_TOLK_CHECKBOX_FIELD_ID] as? Boolean ?: false,
        tolkSprak = fieldValues[TOLK_SPRAK_TEXT_FIELD_ID] as? String,
    )
}

fun MotebehovFormValues.toMotebehovFormValuesOutputDTO(): MotebehovFormValuesOutputDTO {
    val valuesFromFormSnapshot = this.formSnapshot?.let { extractValuesFromFormSnapshot(it) }

    return MotebehovFormValuesOutputDTO(
        harMotebehov = this.harMotebehov,
        forklaring = this.forklaring,
        formSnapshot = this.formSnapshot,
        begrunnelse = valuesFromFormSnapshot?.begrunnelse,
        onskerSykmelderDeltar = valuesFromFormSnapshot?.onskerSykmelderDeltar,
        onskerSykmelderDeltarBegrunnelse = valuesFromFormSnapshot?.onskerSykmelderDeltarBegrunnelse,
        onskerTolk = valuesFromFormSnapshot?.onskerTolk,
        tolkSprak = valuesFromFormSnapshot?.tolkSprak,
    )
}

fun MotebehovFormValues.toPMotebehovFormValues(): PMotebehovFormValues? {
    if (this.formSnapshot == null) {
        return null
    }

    val valuesFromFormSnapshot = extractValuesFromFormSnapshot(this.formSnapshot)

    return PMotebehovFormValues(
        formSnapshotJSON = convertFormSnapshotToJson(this.formSnapshot),
        begrunnelse = valuesFromFormSnapshot.begrunnelse,
        onskerSykmelderDeltar = valuesFromFormSnapshot.onskerSykmelderDeltar,
        onskerSykmelderDeltarBegrunnelse = valuesFromFormSnapshot.onskerSykmelderDeltarBegrunnelse,
        onskerTolk = valuesFromFormSnapshot.onskerTolk,
        tolkSprak = valuesFromFormSnapshot.tolkSprak,
    )
}
