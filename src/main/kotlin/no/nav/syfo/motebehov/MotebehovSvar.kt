package no.nav.syfo.motebehov

import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType
import java.io.Serializable

data class MotebehovSvar(
    val harMotebehov: Boolean,
    // This forklaring field is to be phased in favor of formFillout, and eventually removed. Details in plan.
    val forklaring: String? = null,
    val formFillout: List<FilloutField>,
    val skjemaType: MotebehovSkjemaType?,
) : Serializable

// Existing input DTO to phase out. MotebehovSvarFormFilloutInputDTO will take over.
data class NyttMotebehovSvarInputDTO(
    val harMotebehov: Boolean,
    val forklaring: String? = null,
) : Serializable

data class NyttMotebehovSvarFormFilloutInputDTO(
    val harMotebehov: Boolean,
    val formFillout: List<FilloutField>,
) : Serializable

data class TemporaryCombinedNyttMotebehovSvar(
    val harMotebehov: Boolean,
    val forklaring: String? = null,
    val formFillout: List<FilloutField>,
)
