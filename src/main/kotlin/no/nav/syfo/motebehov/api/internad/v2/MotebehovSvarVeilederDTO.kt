package no.nav.syfo.motebehov.api.internad.v2

import java.io.Serializable

data class MotebehovSvarVeilederDTO(
    val harMotebehov: Boolean,
    val forklaring: String? = null
) : Serializable
