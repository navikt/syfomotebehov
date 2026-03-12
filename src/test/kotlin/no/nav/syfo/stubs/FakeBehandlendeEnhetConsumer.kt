package no.nav.syfo.stubs

import no.nav.syfo.consumer.behandlendeenhet.BehandlendeEnhet
import no.nav.syfo.consumer.behandlendeenhet.EnhetDTO
import no.nav.syfo.consumer.behandlendeenhet.IBehandlendeEnhetConsumer
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("local", "!remote")
@Component
class FakeBehandlendeEnhetConsumer : IBehandlendeEnhetConsumer {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    init {
        logger.warn("!! ----- Running with fake BehandlendeEnhetConsumer  ----- !!")
    }

    override fun getBehandlendeEnhet(fnr: String, callId: String?): BehandlendeEnhet =
        BehandlendeEnhet(
            geografiskEnhet = EnhetDTO("0330", "Bjerke"),
            oppfolgingsenhetDTO = null,
        )
}
