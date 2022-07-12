package no.nav.syfo.varsel

import java.io.Serializable

data class MotebehovsvarVarselInfo(
    val sykmeldtAktorId: String,
    val orgnummer: String,
    val naermesteLederFnr: String,
    val arbeidstakerFnr: String,
) : Serializable

data class MotebehovsvarSykmeldtVarselInfo(
    val sykmeldtAktorId: String,
    val arbeidstakerFnr: String,
    val orgnummer: String?,
) : Serializable
