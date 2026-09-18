package no.nav.syfo.stubs

import no.nav.syfo.consumer.brukertilgang.DineSykmeldteResponse
import no.nav.syfo.consumer.brukertilgang.IDineSykmeldteConsumer
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.UUID

@Profile("local & !remote")
@Component
class FakeDineSykmeldteConsumer : IDineSykmeldteConsumer {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    init {
        logger.warn("!! ----- Running with fake DineSykmeldteConsumer ----- !!")
    }

    override fun getSykmeldt(narmesteLederId: UUID): DineSykmeldteResponse =
        DineSykmeldteResponse(
            fnr = "12345678912",
            orgnummer = "123456789",
        )
}
