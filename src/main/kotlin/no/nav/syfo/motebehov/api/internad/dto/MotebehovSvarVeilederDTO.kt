package no.nav.syfo.motebehov.api.internad.dto

import java.io.Serializable

data class MotebehovSvarVeilederDTO(
    val harMotebehov: Boolean,
    val forklaring: String? = null
) : Serializable
