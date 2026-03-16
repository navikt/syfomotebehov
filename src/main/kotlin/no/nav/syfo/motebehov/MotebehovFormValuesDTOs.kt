package no.nav.syfo.motebehov

import no.nav.syfo.motebehov.formSnapshot.BEGRUNNELSE_TEXT_FIELD_ID
import no.nav.syfo.motebehov.formSnapshot.FormSnapshot
import no.nav.syfo.motebehov.formSnapshot.ONSKER_SYKMELDER_DELTAR_BEGRUNNELSE_TEXT_FIELD_ID
import no.nav.syfo.motebehov.formSnapshot.ONSKER_SYKMELDER_DELTAR_CHECKBOX_FIELD_ID
import no.nav.syfo.motebehov.formSnapshot.ONSKER_TOLK_CHECKBOX_FIELD_ID
import no.nav.syfo.motebehov.formSnapshot.TOLK_SPRAK_TEXT_FIELD_ID

data class MotebehovFormSubmissionDTO(
    val harMotebehov: Boolean,
    val formSnapshot: FormSnapshot,
)

data class MotebehovFormValuesOutputDTO(
    val harMotebehov: Boolean,
    val formSnapshot: FormSnapshot?,
    val begrunnelse: String? = null,
    val onskerSykmelderDeltar: Boolean? = null,
    val onskerSykmelderDeltarBegrunnelse: String? = null,
    val onskerTolk: Boolean? = null,
    val tolkSprak: String? = null,
)

data class MotebehovFormValuesExtractedFromFormSnapshot(
    val formIdentifier: String,
    val formSemanticVersion: String,
    val begrunnelse: String? = null,
    val onskerSykmelderDeltar: Boolean,
    val onskerSykmelderDeltarBegrunnelse: String? = null,
    val onskerTolk: Boolean,
    val tolkSprak: String? = null,
)

fun extractFormValuesFromFormSnapshot(formSnapshot: FormSnapshot): MotebehovFormValuesExtractedFromFormSnapshot {
    val fieldValues = formSnapshot.fieldValues

    return MotebehovFormValuesExtractedFromFormSnapshot(
        formIdentifier = formSnapshot.formIdentifier.identifier,
        formSemanticVersion = formSnapshot.formSemanticVersion,
        begrunnelse = fieldValues[BEGRUNNELSE_TEXT_FIELD_ID] as? String,
        onskerSykmelderDeltar = fieldValues[ONSKER_SYKMELDER_DELTAR_CHECKBOX_FIELD_ID] as? Boolean ?: false,
        onskerSykmelderDeltarBegrunnelse = fieldValues[ONSKER_SYKMELDER_DELTAR_BEGRUNNELSE_TEXT_FIELD_ID] as? String,
        onskerTolk = fieldValues[ONSKER_TOLK_CHECKBOX_FIELD_ID] as? Boolean ?: false,
        tolkSprak = fieldValues[TOLK_SPRAK_TEXT_FIELD_ID] as? String,
    )
}

fun MotebehovFormSubmissionDTO.toMotebehovFormValuesOutputDTO(): MotebehovFormValuesOutputDTO {
    val valuesFromFormSnapshot = extractFormValuesFromFormSnapshot(this.formSnapshot)

    return MotebehovFormValuesOutputDTO(
        harMotebehov = this.harMotebehov,
        formSnapshot = this.formSnapshot,
        begrunnelse = valuesFromFormSnapshot.begrunnelse,
        onskerSykmelderDeltar = valuesFromFormSnapshot.onskerSykmelderDeltar,
        onskerSykmelderDeltarBegrunnelse = valuesFromFormSnapshot.onskerSykmelderDeltarBegrunnelse,
        onskerTolk = valuesFromFormSnapshot.onskerTolk,
        tolkSprak = valuesFromFormSnapshot.tolkSprak,
    )
}
