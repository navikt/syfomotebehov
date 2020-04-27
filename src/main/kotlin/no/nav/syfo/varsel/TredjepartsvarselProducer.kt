package no.nav.syfo.varsel

import no.nav.syfo.kafka.producer.model.KTredjepartsvarsel
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.*
import javax.inject.Inject

@Component
class TredjepartsvarselProducer @Inject constructor(
        private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    fun sendTredjepartsvarselvarsel(kTredjepartsvarsel: KTredjepartsvarsel) {
        try {
            kafkaTemplate.send(
                    TREDJEPARTSVARSEL_TOPIC,
                    UUID.randomUUID().toString(),
                    kTredjepartsvarsel).get()
            log.info("Legger tredjepartsvarsel med ressursID {} på kø for aktor {}", kTredjepartsvarsel.ressursId, kTredjepartsvarsel.aktorId)
        } catch (e: Exception) {
            log.error("Feil ved sending av oppgavevarsel", e)
            throw RuntimeException("Feil ved sending av oppgavevarsel", e)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(TredjepartsvarselProducer::class.java)
        const val TREDJEPARTSVARSEL_TOPIC = "aapen-syfo-tredjepartsvarsel-v1"
    }
}
