package no.nav.syfo.motebehov.motebehovstatus

import no.nav.syfo.motebehov.Motebehov
import no.nav.syfo.motebehov.isUbehandlet
import java.io.Serializable

data class MotebehovStatus(
    val visMotebehov: Boolean,
    val skjemaType: MotebehovSkjemaType? = null,
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
