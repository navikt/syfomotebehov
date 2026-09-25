package no.nav.syfo.oppfolgingstilfelle.kafka

import com.fasterxml.jackson.core.JsonProcessingException
import no.nav.esyfo.observability.rethrowIfCancelled
import no.nav.syfo.consumer.pdl.PdlRequestFailedException
import no.nav.syfo.consumer.pdl.withPdlDiagnostics
import no.nav.syfo.testdata.reset.TestdataResetService
import no.nav.syfo.util.failureKind
import no.nav.syfo.util.withFailureDiagnostics
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
            log
                .atError()
                .addKeyValue("event_type", "testdata_reset_decode_failed")
                .addKeyValue("operation", "testdata_reset_consume")
                .addKeyValue("upstream", "kafka")
                .addKeyValue("failure_stage", "message_processing")
                .addKeyValue("failure_kind", e.failureKind().value)
                .withFailureDiagnostics(e)
                .log("Could not decode a test data reset message")
        } catch (e: PdlRequestFailedException) {
            e.rethrowIfCancelled()
            log.atError().withPdlDiagnostics(e).log("Could not reset test data because the PDL lookup failed")
        } catch (e: Exception) {
            e.rethrowIfCancelled()
            log
                .atError()
                .addKeyValue("event_type", "testdata_reset_failed")
                .addKeyValue("operation", "testdata_reset_consume")
                .addKeyValue("upstream", "kafka")
                .addKeyValue("failure_stage", "message_processing")
                .addKeyValue("failure_kind", e.failureKind().value)
                .withFailureDiagnostics(e)
                .log("Could not reset test data from a message")
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(TestdataResetListener::class.java)

        private const val TESTDATA_RESET_TOPIC = "teamsykefravr.testdata-reset"
    }
}
