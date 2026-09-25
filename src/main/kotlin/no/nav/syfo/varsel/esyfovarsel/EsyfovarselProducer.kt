package no.nav.syfo.varsel.esyfovarsel

import no.nav.syfo.util.failureKind
import no.nav.syfo.util.rethrowIfCancelled
import no.nav.syfo.util.withFailureDiagnostics
import no.nav.syfo.varsel.esyfovarsel.domain.EsyfovarselHendelse
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class EsyfovarselProducer
    @Autowired
    constructor(
        @Qualifier("EsyfovarselKafkaTemplate") private val kafkaTemplate: KafkaTemplate<String, EsyfovarselHendelse>,
    ) {
        fun sendVarselTilEsyfovarsel(esyfovarselHendelse: EsyfovarselHendelse) {
            try {
                log.info("EsyfovarselProducer: Sender varsel av type ${esyfovarselHendelse.type.name}")
                kafkaTemplate
                    .send(
                        ProducerRecord(
                            ESYFOVARSEL_TOPIC,
                            UUID.randomUUID().toString(),
                            esyfovarselHendelse,
                        ),
                    ).get()
            } catch (e: Exception) {
                e.rethrowIfCancelled()
                log
                    .atError()
                    .addKeyValue("event_type", "varsel_publish_failed")
                    .addKeyValue("operation", "varsel_publish")
                    .addKeyValue("upstream", "kafka")
                    .addKeyValue("failure_stage", "message_publish")
                    .addKeyValue("failure_kind", e.failureKind().value)
                    .withFailureDiagnostics(e)
                    .log("Could not publish a notification to varselbus")
                throw e
            }
        }

        companion object {
            const val ESYFOVARSEL_TOPIC = "team-esyfo.varselbus"
            private val log = LoggerFactory.getLogger(EsyfovarselProducer::class.java)
        }
    }
