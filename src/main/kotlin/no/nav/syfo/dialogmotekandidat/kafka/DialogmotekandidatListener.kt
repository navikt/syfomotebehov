package no.nav.syfo.dialogmotekandidat.kafka

import com.fasterxml.jackson.core.JsonProcessingException
import no.nav.syfo.dialogmotekandidat.DialogmotekandidatService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Profile("remote")
@Component
class DialogmotekandidatListener(
    private val dialogmotekandidatService: DialogmotekandidatService
) {
    @KafkaListener(topics = [DIALOGMOTEKANDIDAT_TOPIC], containerFactory = "DialogmotekandidatListenerContainerFactory")
    fun dialogmoteStatusEndringListener(
        consumerRecord: ConsumerRecord<String, KafkaDialogmotekandidatEndring>,
        acknowledgment: Acknowledgment
    ) {
        log.info("Got record from $DIALOGMOTEKANDIDAT_TOPIC topic for uuid: ${consumerRecord.value().uuid}")
        try {
            dialogmotekandidatService.receiveDialogmotekandidatEndring(consumerRecord.value())
            acknowledgment.acknowledge()
        } catch (e: JsonProcessingException) {
            log.error("DialogmotekandidatListener: Kunne ikke deserialisere topic", e)
        } catch (e: Exception) {
            log.error("DialogmotekandidatListener: Uventet feil ved lesing av topic", e)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(DialogmotekandidatListener::class.java)
        const val DIALOGMOTEKANDIDAT_TOPIC = "teamsykefravr.isdialogmotekandidat-dialogmotekandidat"
    }
}
