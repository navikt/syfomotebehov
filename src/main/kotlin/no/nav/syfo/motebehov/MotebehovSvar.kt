package no.nav.syfo.motebehov

import no.nav.syfo.motebehov.database.PMotebehovSvar
import no.nav.syfo.motebehov.database.convertFormFilloutToJson
import no.nav.syfo.motebehov.formFillout.BEGRUNNELSE_TEXT_FIELD_ID
import no.nav.syfo.motebehov.formFillout.FormFillout
import no.nav.syfo.motebehov.formFillout.ONSKER_SYKMELDER_DELTAR_BEGRUNNELSE_TEXT_FIELD_ID
import no.nav.syfo.motebehov.formFillout.ONSKER_SYKMELDER_DELTAR_CHECKBOX_FIELD_ID
import no.nav.syfo.motebehov.formFillout.ONSKER_TOLK_CHECKBOX_FIELD_ID
import no.nav.syfo.motebehov.formFillout.TOLK_SPRAK_TEXT_FIELD_ID
import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType
import java.util.*

data class MotebehovSvar(
    val harMotebehov: Boolean,
    // This forklaring field is to be phased in favor of formFillout, and eventually removed. Details in plan.
    val forklaring: String? = null,
    val formFillout: FormFillout?
)

// Existing input DTO to phase out. MotebehovSvarFormFilloutInputDTO will take over.
data class MotebehovSvarInputDTO(
    val harMotebehov: Boolean,
    val forklaring: String? = null,
)

data class MotebehovSvarFormFilloutInputDTO(
    val harMotebehov: Boolean,
    val formFillout: FormFillout,
    // New fields to get on input and store, instead of having to calculate them in a probably unstable way.
    val skjemaType: MotebehovSkjemaType,
    val innmelderType: MotebehovInnmelderType,
)

data class TemporaryCombinedNyttMotebehovSvar(
    val harMotebehov: Boolean,
    val forklaring: String? = null,
    val formFillout: FormFillout?,
)

data class MotebehovSvarOutputDTO(
    val harMotebehov: Boolean,
    val forklaring: String? = null,
    val formFillout: FormFillout?,
    val begrunnelse: String? = null,
    val onskerSykmelderDeltar: Boolean? = null,
    val onskerSykmelderDeltarBegrunnelse: String? = null,
    val onskerTolk: Boolean? = null,
    val tolkSprak: String? = null,
)

data class MotebehovSvarFieldsFromFormFillout(
    val begrunnelse: String? = null,
    val onskerSykmelderDeltar: Boolean,
    val onskerSykmelderDeltarBegrunnelse: String? = null,
    val onskerTolk: Boolean,
    val tolkSprak: String? = null,
)

fun extractValuesFromFormFillout(formFillout: FormFillout): MotebehovSvarFieldsFromFormFillout {
    val fieldValues = formFillout.fieldValues

    return MotebehovSvarFieldsFromFormFillout(
        begrunnelse = fieldValues[BEGRUNNELSE_TEXT_FIELD_ID] as? String,
        onskerSykmelderDeltar = fieldValues[ONSKER_SYKMELDER_DELTAR_CHECKBOX_FIELD_ID] as? Boolean ?: false,
        onskerSykmelderDeltarBegrunnelse = fieldValues[ONSKER_SYKMELDER_DELTAR_BEGRUNNELSE_TEXT_FIELD_ID] as? String,
        onskerTolk = fieldValues[ONSKER_TOLK_CHECKBOX_FIELD_ID] as? Boolean ?: false,
        tolkSprak = fieldValues[TOLK_SPRAK_TEXT_FIELD_ID] as? String,
    )
}

fun MotebehovSvar.toMotebehovSvarOutputDTO(): MotebehovSvarOutputDTO {
    val valuesFromFormFillout = this.formFillout?.let { extractValuesFromFormFillout(it) }

    return MotebehovSvarOutputDTO(
        harMotebehov = this.harMotebehov,
        forklaring = this.forklaring,
        formFillout = this.formFillout,
        begrunnelse = valuesFromFormFillout?.begrunnelse,
        onskerSykmelderDeltar = valuesFromFormFillout?.onskerSykmelderDeltar,
        onskerSykmelderDeltarBegrunnelse = valuesFromFormFillout?.onskerSykmelderDeltarBegrunnelse,
        onskerTolk = valuesFromFormFillout?.onskerTolk,
        tolkSprak = valuesFromFormFillout?.tolkSprak,
    )
}

fun MotebehovSvar.toPMotebehovSvar(): PMotebehovSvar? {
    if (this.formFillout == null) {
        return null
    }

    val valuesFromFormFillout = extractValuesFromFormFillout(this.formFillout)

    return PMotebehovSvar(
        uuid = UUID.randomUUID(),
        formFilloutJSON = convertFormFilloutToJson(this.formFillout),
        begrunnelse = valuesFromFormFillout.begrunnelse,
        onskerSykmelderDeltar = valuesFromFormFillout.onskerSykmelderDeltar,
        onskerSykmelderDeltarBegrunnelse = valuesFromFormFillout.onskerSykmelderDeltarBegrunnelse,
        onskerTolk = valuesFromFormFillout.onskerTolk,
        tolkSprak = valuesFromFormFillout.tolkSprak,
    )
}
