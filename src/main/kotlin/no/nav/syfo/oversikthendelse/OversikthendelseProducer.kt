package no.nav.syfo.oversikthendelse

import no.nav.syfo.kafka.producer.model.KOversikthendelse
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.*
import javax.inject.Inject

@Component
class OversikthendelseProducer @Inject constructor(
        private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    fun sendOversikthendelse(kOversikthendelse: KOversikthendelse) {
        try {
            kafkaTemplate.send(
                    OVERSIKTHENDELSE_TOPIC,
                    UUID.randomUUID().toString(),
                    kOversikthendelse
            ).get()
            log.info("Legger oversikthendelse med id {} på kø for enhet {}", kOversikthendelse.hendelseId, kOversikthendelse.enhetId)
        } catch (e: Exception) {
            log.error("Feil ved sending av oppgavevarsel", e)
            throw RuntimeException("Feil ved sending av oppgavevarsel", e)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(OversikthendelseProducer::class.java)
        const val OVERSIKTHENDELSE_TOPIC = "aapen-syfo-oversikthendelse-v1"
    }
}
