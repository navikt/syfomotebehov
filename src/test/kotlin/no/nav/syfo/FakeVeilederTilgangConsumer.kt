package no.nav.syfo

import no.nav.syfo.consumer.veiledertilgang.IVeilederTilgangConsumer
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("local", "!remote")
@Component
class FakeVeilederTilgangConsumer: IVeilederTilgangConsumer {
    private val logger = LoggerFactory.getLogger(this.javaClass)
    init {
        logger.warn("!! ----- Running with fake VeilederTilgangConsumer  ----- !!")
    }

    override fun sjekkVeiledersTilgangTilPersonMedOBO(fnr: String): Boolean = true
}
