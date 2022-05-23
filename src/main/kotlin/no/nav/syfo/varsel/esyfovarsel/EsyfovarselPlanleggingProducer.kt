package no.nav.syfo.varsel.esyfovarsel

import no.nav.syfo.varsel.esyfovarsel.domain.EsyfovarselPlanlagtVarsel
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.*

@Component
class EsyfovarselPlanleggingProducer @Autowired constructor(
    @Qualifier("EsyfovarselKafkaTemplate") private val kafkaTemplate: KafkaTemplate<String, EsyfovarselPlanlagtVarsel>,
) {
    fun sendVarselTilEsyfovarselPlanlegging(
        esyfovarselPlanlagtVarsel: EsyfovarselPlanlagtVarsel,
    ) {
        try {
            kafkaTemplate.send(
                ProducerRecord(
                    ESYFOVARSEL_TOPIC,
                    UUID.randomUUID().toString(),
                    esyfovarselPlanlagtVarsel,
                )
            ).get()
        } catch (e: Exception) {
            log.error("Exception was thrown when attempting to send varsel to esyfovarsel-planlegging. ${e.message}")
            throw e
        }
    }

    companion object {
        private const val ESYFOVARSEL_TOPIC = "team-esyfo.varsel-planlegging"
        private val log = LoggerFactory.getLogger(EsyfovarselPlanleggingProducer::class.java)
    }
}
