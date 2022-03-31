package no.nav.syfo.varsel.dinesykmeldte

import no.nav.syfo.varsel.dinesykmeldte.domain.DineSykmeldteHendelse
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.*
import javax.inject.Inject

@Component
class DineSykmeldteVarselProducer @Inject constructor(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    fun sendDineSykmeldteVarsel(
        dineSykmeldteHendelse: DineSykmeldteHendelse,
    ) {
        try {
            kafkaTemplate.send(
                ProducerRecord(
                    DINESYKMELDTE_HENDELSE_TOPIC,
                    UUID.randomUUID().toString(),
                    dineSykmeldteHendelse,
                )
            ).get()
        } catch (e: Exception) {
            log.error("Feil ved sending av varsel til dine sykmeldte", e)
            throw RuntimeException("Feil ved sending av varsel til dine sykmeldte", e)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(DineSykmeldteVarselProducer::class.java)
        const val DINESYKMELDTE_HENDELSE_TOPIC = "teamsykmelding.dinesykmeldte-hendelser-v2"
    }
}
