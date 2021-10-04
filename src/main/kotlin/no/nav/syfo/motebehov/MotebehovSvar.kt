package no.nav.syfo.motebehov

import no.nav.syfo.motebehov.api.internad.v2.MotebehovSvarVeilederDTO
import java.io.Serializable

data class MotebehovSvar(
    val harMotebehov: Boolean,
    val forklaring: String? = null
) : Serializable

fun MotebehovSvar.toMotebehovVeilederDTO() =
    MotebehovSvarVeilederDTO(
        harMotebehov = this.harMotebehov,
        forklaring = this.forklaring
    )
