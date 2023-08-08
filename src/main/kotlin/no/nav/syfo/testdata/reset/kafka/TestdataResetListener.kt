package no.nav.syfo.oppfolgingstilfelle.kafka

import com.fasterxml.jackson.core.JsonProcessingException
import no.nav.syfo.testdata.reset.TestdataResetService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Profile("testdatareset")
@Component
class TestdataResetListener(
    private val testdataResetService: TestdataResetService,
) {

    @KafkaListener(
        topics = [TESTDATA_RESET_TOPIC],
        containerFactory = "TestdataResetListenerContainerFactory",
    )
    fun testdataResetListener(
        consumerRecord: ConsumerRecord<String, String>,
        acknowledgment: Acknowledgment,
    ) {
        try {
            val fnr = consumerRecord.value()
            testdataResetService.resetTestdata(fnr)
            acknowledgment.acknowledge()
        } catch (e: JsonProcessingException) {
            log.error("TestdataResetListener: Kunne ikke deserialisere record", e)
        } catch (e: Exception) {
            log.error("TestdataResetListener: Uventet feil ved lesing av record", e)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(TestdataResetListener::class.java)

        private const val TESTDATA_RESET_TOPIC = "teamsykefravr.testdata-reset"
    }
}
