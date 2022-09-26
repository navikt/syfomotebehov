package no.nav.syfo.varsel.esyfovarsel

import no.nav.syfo.varsel.esyfovarsel.domain.EsyfovarselHendelse
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.*

@Component
class EsyfovarselProducer @Autowired constructor(
    @Qualifier("EsyfovarselKafkaTemplate") private val kafkaTemplate: KafkaTemplate<String, EsyfovarselHendelse>,
) {
    fun sendVarselTilEsyfovarsel(
        esyfovarselHendelse: EsyfovarselHendelse,
    ) {
        try {
            log.info("EsyfovarselProducer: Sender varsel av type ${esyfovarselHendelse.type.name}")
            kafkaTemplate.send(
                ProducerRecord(
                    ESYFOVARSEL_TOPIC,
                    UUID.randomUUID().toString(),
                    esyfovarselHendelse,
                )
            ).get()
        } catch (e: Exception) {
            log.error("Exception was thrown when attempting to send varsel to esyfovarsel. ${e.message}")
            throw e
        }
    }

    companion object {
        const val ESYFOVARSEL_TOPIC = "team-esyfo.varselbus"
        private val log = LoggerFactory.getLogger(EsyfovarselProducer::class.java)
    }
}
