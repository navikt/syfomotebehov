package no.nav.syfo.consumer.brukertilgang

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.UUID

interface IDineSykmeldteConsumer {
    fun getSykmeldt(narmesteLederId: UUID): DineSykmeldteResponse?
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class DineSykmeldteResponse(
    val fnr: String,
    val orgnummer: String,
)
