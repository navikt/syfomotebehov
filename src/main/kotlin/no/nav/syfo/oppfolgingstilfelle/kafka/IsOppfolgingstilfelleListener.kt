package no.nav.syfo.oppfolgingstilfelle.kafka

import com.fasterxml.jackson.core.JsonProcessingException
import no.nav.syfo.oppfolgingstilfelle.OppfolgingstilfelleService
import no.nav.syfo.oppfolgingstilfelle.kafka.domain.KafkaOppfolgingstilfellePerson
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment

@Profile("remote")
class IsOppfolgingstilfelleListener(
    private val oppfolgingstilfelleService: OppfolgingstilfelleService
) {

    @KafkaListener(
        topics = [ISOPPFOLGINGSTILFELLE_TOPIC],
        containerFactory = "IsOppfolgingstilfelleListenerContainerFactory"
    )
    fun oppfolgingstilfellePekerListener(
        consumerRecord: ConsumerRecord<String, KafkaOppfolgingstilfellePerson>,
        acknowledgment: Acknowledgment
    ) {
        try {
            val oppfolgingstilfellePerson = consumerRecord.value()
            oppfolgingstilfelleService.receiveKOppfolgingstilfelle(oppfolgingstilfellePerson)
            acknowledgment.acknowledge()
        } catch (e: JsonProcessingException) {
            log.error("IsOppfolgingstilfelleListener: Kunne ikke deserialisere oppfolgingstilfelle record", e)
        } catch (e: Exception) {
            log.error("IsOppfolgingstilfelleListener: Uventet feil ved lesing av oppfolgingstilfelle record", e)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(IsOppfolgingstilfelleListener::class.java)

        private const val ISOPPFOLGINGSTILFELLE_TOPIC = "teamsykefravr.isoppfolgingstilfelle-oppfolgingstilfelle-person"
    }
}
