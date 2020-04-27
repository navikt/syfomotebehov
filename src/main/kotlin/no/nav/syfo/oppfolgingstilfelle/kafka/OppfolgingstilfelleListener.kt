package no.nav.syfo.oppfolgingstilfelle.kafka

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.syfo.oppfolgingstilfelle.OppfolgingstilfelleService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.io.IOException

private val objectMapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .registerModule(KotlinModule())
        .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE, true)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

@Profile("remote")
@Component
class OppfolgingstilfelleListener(
        private val oppfolgingstilfelleService: OppfolgingstilfelleService
) {

    @KafkaListener(topics = [OPPFOLGINGSTILFELLE_TOPIC])
    fun oppfolgingstilfellePekerListener(
            consumerRecord: ConsumerRecord<String, String>,
            acknowledgment: Acknowledgment
    ) {
        try {
            val kOppfolgingstilfellePeker: KOppfolgingstilfellePeker = map(consumerRecord.value())
            oppfolgingstilfelleService.receiveKOppfolgingstilfellePeker(kOppfolgingstilfellePeker)
            acknowledgment.acknowledge()
        } catch (e: JsonProcessingException) {
            LOG.error("Kunne ikke deserialisere KOppfolgingstilfellePeker", e)
            throw RuntimeException("Kunne ikke deserialisere KOppfolgingstilfellePeker", e)
        } catch (e: Exception) {
            LOG.error("Uventet feil ved lesing av KOppfolgingstilfellePeker", e)
            throw RuntimeException("Uventet feil lesing av KOppfolgingstilfellePeker", e)
        }
    }

    @Throws(IOException::class)
    private fun map(string: String): KOppfolgingstilfellePeker {
        return objectMapper.readValue(string, KOppfolgingstilfellePeker::class.java)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(OppfolgingstilfelleListener::class.java)

        private const val OPPFOLGINGSTILFELLE_TOPIC = "aapen-syfo-oppfolgingstilfelle-v1"
    }
}