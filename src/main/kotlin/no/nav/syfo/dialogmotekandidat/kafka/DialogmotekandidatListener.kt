package no.nav.syfo.dialogmotekandidat.kafka

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.syfo.dialogmotekandidat.DialogmotekandidatService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

/**
 * Lytter på Kafka-topic [DIALOGMOTEKANDIDAT_TOPIC] og delegerer til [DialogmotekandidatService].
 *
 * Se [docs/dialogmotekandidat-varsel-flow.md](../../../../../../../../docs/dialogmotekandidat-varsel-flow.md)
 * for full beskrivelse av flyten.
 */
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
        try {
            val melding = consumerRecord.value()
            log.info(
                "Got record",
                kv("event", "dialogmotekandidat.received"),
                kv("topic", DIALOGMOTEKANDIDAT_TOPIC),
                kv("uuid", melding.uuid),
            )
            dialogmotekandidatService.receiveDialogmotekandidatEndring(melding)
            acknowledgment.acknowledge()
        } catch (e: Exception) {
            log.error("DialogmotekandidatListener: Uventet feil ved lesing av topic", e)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(DialogmotekandidatListener::class.java)
        const val DIALOGMOTEKANDIDAT_TOPIC = "teamsykefravr.isdialogmotekandidat-dialogmotekandidat"
    }
}
