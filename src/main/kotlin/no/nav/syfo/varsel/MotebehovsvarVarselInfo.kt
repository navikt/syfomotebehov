package no.nav.syfo.varsel

import java.io.Serializable

data class MotebehovsvarVarselInfo(
        val sykmeldtAktorId: String,
        val orgnummer: String
) : Serializable
