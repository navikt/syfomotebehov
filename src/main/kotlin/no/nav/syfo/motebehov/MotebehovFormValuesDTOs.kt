package no.nav.syfo.motebehov

import no.nav.syfo.motebehov.database.PMotebehovFormValues
import no.nav.syfo.motebehov.formSnapshot.BEGRUNNELSE_TEXT_FIELD_ID
import no.nav.syfo.motebehov.formSnapshot.FormSnapshot
import no.nav.syfo.motebehov.formSnapshot.ONSKER_SYKMELDER_DELTAR_BEGRUNNELSE_TEXT_FIELD_ID
import no.nav.syfo.motebehov.formSnapshot.ONSKER_SYKMELDER_DELTAR_CHECKBOX_FIELD_ID
import no.nav.syfo.motebehov.formSnapshot.ONSKER_TOLK_CHECKBOX_FIELD_ID
import no.nav.syfo.motebehov.formSnapshot.TOLK_SPRAK_TEXT_FIELD_ID
import no.nav.syfo.motebehov.formSnapshot.convertFormSnapshotToJsonString

// Temporary class used in services to contain values of both legacy and new form submission DTOs below.
// To be replaced by MotebehovFormSubmissionInputDTO.
data class MotebehovFormSubmissionCombinedDTO(
    val harMotebehov: Boolean,
    val forklaring: String? = null,
    val formSnapshot: FormSnapshot?
)

// Existing input DTO to phase out. MotebehovFormValuesInputDTO will take over.
data class MotebehovSvarLegacyInputDTO(
    val harMotebehov: Boolean,
    val forklaring: String? = null,
)

data class MotebehovFormSubmissionInputDTO(
    val harMotebehov: Boolean,
    val formSnapshot: FormSnapshot,
)

data class MotebehovFormValuesOutputDTO(
    val harMotebehov: Boolean,
    // forklaring field is to be phased out
    val forklaring: String? = null,
    val formSnapshot: FormSnapshot?,
    val begrunnelse: String? = null,
    val onskerSykmelderDeltar: Boolean? = null,
    val onskerSykmelderDeltarBegrunnelse: String? = null,
    val onskerTolk: Boolean? = null,
    val tolkSprak: String? = null,
)

data class MotebehovFormValuesExtractedFromFormSnapshot(
    val begrunnelse: String? = null,
    val onskerSykmelderDeltar: Boolean,
    val onskerSykmelderDeltarBegrunnelse: String? = null,
    val onskerTolk: Boolean,
    val tolkSprak: String? = null,
)

fun extractValuesFromFormSnapshot(formSnapshot: FormSnapshot): MotebehovFormValuesExtractedFromFormSnapshot {
    val fieldValues = formSnapshot.fieldValues

    return MotebehovFormValuesExtractedFromFormSnapshot(
        begrunnelse = fieldValues[BEGRUNNELSE_TEXT_FIELD_ID] as? String,
        onskerSykmelderDeltar = fieldValues[ONSKER_SYKMELDER_DELTAR_CHECKBOX_FIELD_ID] as? Boolean ?: false,
        onskerSykmelderDeltarBegrunnelse = fieldValues[ONSKER_SYKMELDER_DELTAR_BEGRUNNELSE_TEXT_FIELD_ID] as? String,
        onskerTolk = fieldValues[ONSKER_TOLK_CHECKBOX_FIELD_ID] as? Boolean ?: false,
        tolkSprak = fieldValues[TOLK_SPRAK_TEXT_FIELD_ID] as? String,
    )
}

fun MotebehovFormSubmissionCombinedDTO.toMotebehovFormValuesOutputDTO(): MotebehovFormValuesOutputDTO {
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

fun MotebehovFormSubmissionCombinedDTO.toPMotebehovFormValues(): PMotebehovFormValues? {
    if (this.formSnapshot == null) {
        return null
    }

    val valuesFromFormSnapshot = extractValuesFromFormSnapshot(this.formSnapshot)

    return PMotebehovFormValues(
        formSnapshotJSON = convertFormSnapshotToJsonString(this.formSnapshot),
        begrunnelse = valuesFromFormSnapshot.begrunnelse,
        onskerSykmelderDeltar = valuesFromFormSnapshot.onskerSykmelderDeltar,
        onskerSykmelderDeltarBegrunnelse = valuesFromFormSnapshot.onskerSykmelderDeltarBegrunnelse,
        onskerTolk = valuesFromFormSnapshot.onskerTolk,
        tolkSprak = valuesFromFormSnapshot.tolkSprak,
    )
}
