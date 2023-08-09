package no.nav.syfo.motebehov

import java.io.Serializable

data class MotebehovVarselVurdering(
    val sendVarselTilSykmeldt: Boolean,
    val sendVarselTilArbeidsgiver: Boolean,
    val varseltekst: String,
    val narmesteLederFnr: String,
    val arbeidstakerFnr: String,
    val orgnummer: String
) : Serializable
