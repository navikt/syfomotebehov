package no.nav.syfo.motebehov

import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType
import java.io.Serializable

data class MotebehovSvar(
    val harMotebehov: Boolean,
    // This forklaring field is to be phased in favor of formFillout, and eventually removed. Details in plan.
    val forklaring: String? = null,
    val formFillout: FormFillout?
) : Serializable

// Existing input DTO to phase out. MotebehovSvarFormFilloutInputDTO will take over.
data class MotebehovSvarInputDTO(
    val harMotebehov: Boolean,
    val forklaring: String? = null,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

data class MotebehovSvarFormFilloutInputDTO(
    val harMotebehov: Boolean,
    val formFillout: FormFillout,
    // New fields to get on input and store, instead of having to calculate them in a probably unstable way.
    val skjemaType: MotebehovSkjemaType,
    val innmelderType: MotebehovInnmelderType,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

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

fun MotebehovSvar.toMotebehovSvarOutputDTO(): MotebehovSvarOutputDTO {
    return MotebehovSvarOutputDTO(
        harMotebehov = this.harMotebehov,
        forklaring = this.forklaring,
        formFillout = this.formFillout,
        begrunnelse = this.formFillout?.fieldValues?.get("begrunnelseText") as? String,
        onskerSykmelderDeltar = this.formFillout?.fieldValues?.get("onskerSykmelderDeltarCheckbox") as? Boolean,
        onskerSykmelderDeltarBegrunnelse =
        this.formFillout?.fieldValues?.get("onskerSykmelderDeltarBegrunnelseText") as? String,
        onskerTolk = this.formFillout?.fieldValues?.get("onskerTolkCheckbox") as? Boolean,
        tolkSprak = this.formFillout?.fieldValues?.get("tolkSprakText") as? String,
    )
}
