package no.nav.syfo.motebehov.motebehovstatus

import no.nav.syfo.motebehov.Motebehov
import java.io.Serializable

data class MotebehovStatus(
        val visMotebehov: Boolean,
        val skjemaType: MotebehovSkjemaType? = null,
        val motebehov: Motebehov? = null
) : Serializable
