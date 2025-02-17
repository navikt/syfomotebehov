package no.nav.syfo.motebehov

import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType
import java.io.Serializable

data class MotebehovSvar(
    val harMotebehov: Boolean,
    // This forklaring field is to be phased in favor of formFillout, and eventually removed. Details in plan.
    val forklaring: String? = null,
    val formFillout: FormFillout?,
    val skjemaType: MotebehovSkjemaType?
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
    val creatorRole: MotebehovCreatorRole,
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
