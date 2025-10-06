package no.nav.syfo.motebehov.motebehovstatus

import no.nav.syfo.motebehov.Motebehov
import no.nav.syfo.motebehov.MotebehovWithFormValuesOutputDTO
import no.nav.syfo.motebehov.isUbehandlet
import no.nav.syfo.motebehov.toMotebehovWithFormValuesOutputDTO
import java.io.Serializable

data class MotebehovStatus(
    // Indikerer om bruker skal ha tilgang til å svare/melde motebehov, eller se sitt svar.
    val visMotebehov: Boolean,
    // Indikerer hvilket skjema bruker skal få opp når visMotebehov er true og motebehov er null.
    val skjemaType: MotebehovSkjemaType,
    val motebehov: Motebehov? = null
) : Serializable

fun MotebehovStatus.isSvarBehovVarselAvailable(): Boolean {
    return this.visMotebehov &&
        this.skjemaType == MotebehovSkjemaType.SVAR_BEHOV &&
        this.motebehov == null
}

fun MotebehovStatus.isSvarBehovVarselAvailable(newestMotebehov: Motebehov): Boolean {
    return this.visMotebehov &&
        this.skjemaType == MotebehovSkjemaType.SVAR_BEHOV &&
        this.motebehov == null &&
        !newestMotebehov.isUbehandlet()
}

fun MotebehovStatus.isMotebehovAvailableForAnswer(): Boolean {
    return this.visMotebehov &&
        this.motebehov == null
}

data class MotebehovStatusWithFormValuesDTO(
    val visMotebehov: Boolean,
    val skjemaType: MotebehovSkjemaType,
    val motebehovWithFormValues: MotebehovWithFormValuesOutputDTO? = null,
)

fun MotebehovStatus.toMotebehovStatusWithFormValuesDTO(): MotebehovStatusWithFormValuesDTO {
    return MotebehovStatusWithFormValuesDTO(
        visMotebehov = this.visMotebehov,
        skjemaType = this.skjemaType,
        motebehovWithFormValues = this.motebehov?.toMotebehovWithFormValuesOutputDTO()
    )
}
