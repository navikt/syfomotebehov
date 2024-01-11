package no.nav.syfo.dialogmote.kafka

import com.fasterxml.jackson.core.JsonProcessingException
import no.nav.syfo.dialogmote.DialogmoteStatusService
import no.nav.syfo.dialogmote.avro.KDialogmoteStatusEndring
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment

@Profile("remote")
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
            LOG.error("DialogmoteStatusendringListener: Kunne ikke deserialisere DM topic", e)
        } catch (e: Exception) {
            LOG.error("DialogmoteStatusendringListener: Uventet feil ved lesing av DM Topic", e)
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(DialogmoteStatusendringListener::class.java)
        private const val DIALOGMOTE_STATUSENDRING_TOPIC = "teamsykefravr.isdialogmote-dialogmote-statusendring"
    }
}
