package no.nav.syfo.motebehov

import java.io.Serializable

data class MotebehovStatus(
        val visMotebehov: Boolean,
        val skjemaType: MotebehovSkjemaType? = null,
        val motebehov: Motebehov? = null
) : Serializable
