package no.nav.syfo.personoppgavehendelse

import no.nav.esyfo.observability.rethrowIfCancelled
import no.nav.syfo.personoppgavehendelse.domain.KPersonoppgavehendelse
import no.nav.syfo.util.failureKind
import no.nav.syfo.util.withFailureDiagnostics
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class PersonoppgavehendelseProducer
    @Autowired
    constructor(
        @Qualifier("PersonoppgavehendelseTemplate") private val kafkaTemplate: KafkaTemplate<String, KPersonoppgavehendelse>,
    ) {
        fun sendPersonoppgavehendelse(
            personoppgaveId: UUID,
            kPersonoppgavehendelse: KPersonoppgavehendelse,
        ) {
            try {
                log.info("Sending personoppgavehendelse of type ${kPersonoppgavehendelse.hendelsetype}, personoppgaveId: $personoppgaveId")
                val record =
                    ProducerRecord(
                        PERSONOPPGAVEHENDELSE_TOPIC,
                        personoppgaveId.toString(),
                        kPersonoppgavehendelse,
                    )
                kafkaTemplate.send(record).get()
            } catch (e: Exception) {
                e.rethrowIfCancelled()
                log
                    .atError()
                    .addKeyValue("event_type", "personoppgave_publish_failed")
                    .addKeyValue("operation", "personoppgave_publish")
                    .addKeyValue("upstream", "kafka")
                    .addKeyValue("failure_stage", "message_publish")
                    .addKeyValue("failure_kind", e.failureKind().value)
                    .withFailureDiagnostics(e)
                    .log("Could not publish a person task event")
                throw e
            }
        }

        companion object {
            private val log: Logger = LoggerFactory.getLogger(PersonoppgavehendelseProducer::class.java)
            const val PERSONOPPGAVEHENDELSE_TOPIC = "teamsykefravr.personoppgavehendelse"
        }
    }
