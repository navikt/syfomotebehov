package no.nav.syfo.motebehov

import java.io.Serializable

data class MotebehovTilbakemelding(
    val varseltekst: String,
    val motebehovId: String,
) : Serializable
