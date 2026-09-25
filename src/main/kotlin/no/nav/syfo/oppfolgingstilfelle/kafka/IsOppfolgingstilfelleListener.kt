package no.nav.syfo.oppfolgingstilfelle.kafka

import com.fasterxml.jackson.core.JsonProcessingException
import no.nav.esyfo.observability.rethrowIfCancelled
import no.nav.syfo.oppfolgingstilfelle.OppfolgingstilfelleService
import no.nav.syfo.oppfolgingstilfelle.kafka.domain.KafkaOppfolgingstilfellePerson
import no.nav.syfo.util.failureKind
import no.nav.syfo.util.withFailureDiagnostics
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Profile("remote")
@Component
class IsOppfolgingstilfelleListener(
    private val oppfolgingstilfelleService: OppfolgingstilfelleService,
) {
    @KafkaListener(
        topics = [ISOPPFOLGINGSTILFELLE_TOPIC],
        containerFactory = "IsOppfolgingstilfelleListenerContainerFactory",
    )
    fun oppfolgingstilfellePekerListener(
        consumerRecord: ConsumerRecord<String, KafkaOppfolgingstilfellePerson>,
        acknowledgment: Acknowledgment,
    ) {
        try {
            val oppfolgingstilfellePerson = consumerRecord.value()
            oppfolgingstilfelleService.receiveKOppfolgingstilfelle(oppfolgingstilfellePerson)
            acknowledgment.acknowledge()
        } catch (e: JsonProcessingException) {
            log
                .atError()
                .addKeyValue("event_type", "oppfolgingstilfelle_decode_failed")
                .addKeyValue("operation", "oppfolgingstilfelle_consume")
                .addKeyValue("upstream", "kafka")
                .addKeyValue("failure_stage", "message_processing")
                .addKeyValue("failure_kind", e.failureKind().value)
                .withFailureDiagnostics(e)
                .log("Could not decode a follow-up case message")
        } catch (e: Exception) {
            e.rethrowIfCancelled()
            log
                .atError()
                .addKeyValue("event_type", "oppfolgingstilfelle_processing_failed")
                .addKeyValue("operation", "oppfolgingstilfelle_consume")
                .addKeyValue("upstream", "kafka")
                .addKeyValue("failure_stage", "message_processing")
                .addKeyValue("failure_kind", e.failureKind().value)
                .withFailureDiagnostics(e)
                .log("Could not process a follow-up case message")
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(IsOppfolgingstilfelleListener::class.java)

        private const val ISOPPFOLGINGSTILFELLE_TOPIC = "teamsykefravr.isoppfolgingstilfelle-oppfolgingstilfelle-person"
    }
}
