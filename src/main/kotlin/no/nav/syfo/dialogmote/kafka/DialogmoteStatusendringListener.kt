package no.nav.syfo.dialogmote.kafka

import com.fasterxml.jackson.core.JsonProcessingException
import no.nav.syfo.dialogmote.DialogmoteStatusService
import no.nav.syfo.dialogmote.avro.KDialogmoteStatusEndring
import no.nav.syfo.util.failureKind
import no.nav.syfo.util.rethrowIfCancelled
import no.nav.syfo.util.withFailureDiagnostics
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Profile("remote")
@Component
class DialogmoteStatusendringListener(
    private val dialogmoteStatusService: DialogmoteStatusService,
) {
    @KafkaListener(topics = [DIALOGMOTE_STATUSENDRING_TOPIC], containerFactory = "DialogmoteListenerContainerFactory")
    fun dialogmoteStatusEndringListener(
        consumerRecord: ConsumerRecord<String, KDialogmoteStatusEndring>,
        acknowledgment: Acknowledgment,
    ) {
        LOG.info("Got record from $DIALOGMOTE_STATUSENDRING_TOPIC topic for dialogmoteUuid: ${consumerRecord.value().getDialogmoteUuid()}")
        try {
            dialogmoteStatusService.receiveKDialogmoteStatusendring(consumerRecord.value())
            acknowledgment.acknowledge()
        } catch (e: JsonProcessingException) {
            LOG
                .atError()
                .addKeyValue("event_type", "dialogmote_status_decode_failed")
                .addKeyValue("operation", "dialogmote_status_consume")
                .addKeyValue("upstream", "kafka")
                .addKeyValue("failure_stage", "message_processing")
                .addKeyValue("failure_kind", e.failureKind().value)
                .withFailureDiagnostics(e)
                .log("Could not decode a meeting status message")
        } catch (e: Exception) {
            e.rethrowIfCancelled()
            LOG
                .atError()
                .addKeyValue("event_type", "dialogmote_status_processing_failed")
                .addKeyValue("operation", "dialogmote_status_consume")
                .addKeyValue("upstream", "kafka")
                .addKeyValue("failure_stage", "message_processing")
                .addKeyValue("failure_kind", e.failureKind().value)
                .withFailureDiagnostics(e)
                .log("Could not process a meeting status message")
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(DialogmoteStatusendringListener::class.java)
        private const val DIALOGMOTE_STATUSENDRING_TOPIC = "teamsykefravr.isdialogmote-dialogmote-statusendring"
    }
}
