package no.nav.syfo.motebehov

import java.io.Serializable

data class MotebehovSvar(
    val harMotebehov: Boolean,
    val forklaring: String? = null
) : Serializable
