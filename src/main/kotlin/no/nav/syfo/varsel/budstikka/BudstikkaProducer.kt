package no.nav.syfo.varsel.budstikka

import no.nav.budstikka.contract.EncodedDispatch
import no.nav.esyfo.observability.rethrowIfCancelled
import no.nav.syfo.util.failureKind
import no.nav.syfo.util.withFailureDiagnostics
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class BudstikkaProducer(
    @Qualifier("BudstikkaKafkaTemplate") private val kafkaTemplate: KafkaTemplate<String, String>,
) {
    /** Nøkkel og payload inneholder fødselsnummer og skal ikke logges. eventId er en teknisk id. */
    fun send(dispatch: EncodedDispatch) {
        try {
            val record = ProducerRecord(dispatch.topic, dispatch.key, dispatch.value)
            dispatch.headerBytes().forEach { (name, value) -> record.headers().add(name, value) }
            kafkaTemplate.send(record).get()
        } catch (e: Exception) {
            e.rethrowIfCancelled()
            log
                .atError()
                .addKeyValue("event_type", "budstikka_publish_failed")
                .addKeyValue("operation", "budstikka_publish")
                .addKeyValue("upstream", "kafka")
                .addKeyValue("failure_stage", "message_publish")
                .addKeyValue("failure_kind", e.failureKind().value)
                .addKeyValue("event_id", dispatch.eventId.toString())
                .withFailureDiagnostics(e)
                .log("Could not publish a dispatch to Budstikka")
            throw e
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(BudstikkaProducer::class.java)
    }
}
