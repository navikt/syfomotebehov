package no.nav.syfo.personoppgavehendelse

import java.util.*
import no.nav.syfo.personoppgavehendelse.domain.KPersonoppgavehendelse
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class PersonoppgavehendelseProducer @Autowired constructor(
    @Qualifier("PersonoppgavehendelseTemplate") private val kafkaTemplate: KafkaTemplate<String, KPersonoppgavehendelse>,
) {
    fun sendPersonoppgavehendelse(
        kPersonoppgavehendelse: KPersonoppgavehendelse,
        personoppgaveId: UUID,
    ) {
        try {
            log.info("Sending personoppgavehendelse of type ${kPersonoppgavehendelse.hendelsetype}, personoppgaveId: $personoppgaveId")
            val record = ProducerRecord(
                PERSONOPPGAVEHENDELSE_TOPIC,
                personoppgaveId.toString(),
                kPersonoppgavehendelse,
            )
            kafkaTemplate.send(record).get()
        } catch (e: Exception) {
            log.error(
                "Exception was thrown when attempting to send KPersonoppgavehendelse with id {}: ${e.message}",
                personoppgaveId,
            )
            throw e
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(PersonoppgavehendelseProducer::class.java)
        const val PERSONOPPGAVEHENDELSE_TOPIC = "teamsykefravr.personoppgavehendelse"
    }
}
