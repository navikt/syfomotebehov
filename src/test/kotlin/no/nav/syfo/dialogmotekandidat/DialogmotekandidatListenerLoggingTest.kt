package no.nav.syfo.dialogmotekandidat

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.dialogmotekandidat.kafka.DialogmotekandidatListener
import no.nav.syfo.dialogmotekandidat.kafka.KafkaDialogmotekandidatEndring
import no.nav.syfo.testhelper.captureApplicationLogs
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.support.Acknowledgment
import java.time.OffsetDateTime

class DialogmotekandidatListenerLoggingTest :
    FunSpec({
        test("failed Kafka attempt retains event and uuid correlation without person data") {
            val message = KafkaDialogmotekandidatEndring("meeting-uuid", OffsetDateTime.now(), "12345678910", true, "PRIVATE_REASON")
            val service = mockk<DialogmotekandidatService>()
            every { service.receiveDialogmotekandidatEndring(message) } throws RuntimeException("PRIVATE_MESSAGE")
            val logs =
                captureApplicationLogs {
                    shouldThrow<RuntimeException> {
                        DialogmotekandidatListener(service).dialogmotekandidatEndringListener(
                            ConsumerRecord("candidate", 0, 0, "key", message),
                            mockk<Acknowledgment>(relaxed = true),
                        )
                    }
                }
            val failure = logs.single { it["event_type"]?.asText() == "dialogmotekandidat_processing_failed" }
            failure["level"].asText() shouldBe "WARN"
            failure["event"].asText() shouldBe "dialogmotekandidat.failed"
            failure["uuid"].asText() shouldBe "meeting-uuid"
            failure.toString().contains("PRIVATE_") shouldBe false
            failure.toString().contains("12345678910") shouldBe false
        }
    })
