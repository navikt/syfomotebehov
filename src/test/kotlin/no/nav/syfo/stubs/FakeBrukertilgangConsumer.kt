package no.nav.syfo.stubs

import no.nav.syfo.consumer.brukertilgang.IBrukertilgangConsumer
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("local", "!remote")
@Component
class FakeBrukertilgangConsumer : IBrukertilgangConsumer {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    init {
        logger.warn("!! ----- Running with fake BrukertilgangConsumer  ----- !!")
    }

    override fun hasAccessToAnsatt(ansattFnr: String): Boolean = true
}
