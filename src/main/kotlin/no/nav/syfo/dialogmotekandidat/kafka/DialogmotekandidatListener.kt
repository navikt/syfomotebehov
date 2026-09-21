package no.nav.syfo.dialogmotekandidat.kafka

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.syfo.dialogmotekandidat.DialogmotekandidatService
import no.nav.syfo.util.failureKind
import no.nav.syfo.util.rethrowIfCancelled
import no.nav.syfo.util.withFailureDiagnostics
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

/**
 * Lytter på Kafka-topic [DIALOGMOTEKANDIDAT_TOPIC] og delegerer til [DialogmotekandidatService].
 *
 * Se `docs/dialogmotekandidat-varsel-flow.md` for full beskrivelse av flyten.
 */
@Profile("remote")
@Component
class DialogmotekandidatListener(
    private val dialogmotekandidatService: DialogmotekandidatService,
) {
    @KafkaListener(topics = [DIALOGMOTEKANDIDAT_TOPIC], containerFactory = "DialogmotekandidatListenerContainerFactory")
    fun dialogmotekandidatEndringListener(
        consumerRecord: ConsumerRecord<String, KafkaDialogmotekandidatEndring>,
        acknowledgment: Acknowledgment,
    ) {
        val melding = consumerRecord.value()
        try {
            log.info(
                "Got record",
                kv("event", "dialogmotekandidat.received"),
                kv("topic", DIALOGMOTEKANDIDAT_TOPIC),
                kv("uuid", melding.uuid),
            )
            dialogmotekandidatService.receiveDialogmotekandidatEndring(melding)
            acknowledgment.acknowledge()
        } catch (e: Exception) {
            e.rethrowIfCancelled()
            log
                .atWarn()
                .addKeyValue("event_type", "dialogmotekandidat_processing_failed")
                .addKeyValue("operation", "dialogmotekandidat_consume")
                .addKeyValue("upstream", "kafka")
                .addKeyValue("failure_stage", "message_processing")
                .addKeyValue("failure_kind", e.failureKind().value)
                .addKeyValue("outcome", "retrying")
                .withFailureDiagnostics(e)
                .log("Meeting candidate message processing failed; retry policy decides the next attempt")
            throw e
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(DialogmotekandidatListener::class.java)
        const val DIALOGMOTEKANDIDAT_TOPIC = "teamsykefravr.isdialogmotekandidat-dialogmotekandidat"
    }
}
