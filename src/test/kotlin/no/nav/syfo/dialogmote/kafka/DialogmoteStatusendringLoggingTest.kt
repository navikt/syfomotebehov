package no.nav.syfo.dialogmote.kafka

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.consumer.pdl.PdlError
import no.nav.syfo.consumer.pdl.PdlErrorExtension
import no.nav.syfo.consumer.pdl.PdlRequestFailedException
import no.nav.syfo.dialogmote.DialogmoteStatusService
import no.nav.syfo.dialogmote.avro.KDialogmoteStatusEndring
import no.nav.syfo.testhelper.captureApplicationLogs
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.support.Acknowledgment

class DialogmoteStatusendringLoggingTest :
    io.kotest.core.spec.style.FunSpec({
        test("Kafka owner retains HTTP PDL failure diagnostics without logging the exception message") {
            val error = PdlRequestFailedException(message = "PRIVATE_MESSAGE", operation = "aktorid_fetch", upstreamStatus = 503)
            val event = failureEvent(error)
            event["exception_type"].asText() shouldBe "PdlRequestFailedException"
            event["pdl_operation"].asText() shouldBe "aktorid_fetch"
            event["upstream_status"].asInt() shouldBe 503
            event["stack_trace"].asText().contains("PdlRequestFailedException") shouldBe true
            event.toString().contains("PRIVATE_") shouldBe false
        }

        test("Kafka owner retains GraphQL PDL errors but not response data or request details") {
            val error =
                PdlRequestFailedException(
                    message = "PRIVATE_MESSAGE",
                    operation = "aktorid_fetch",
                    pdlErrors =
                        listOf(
                            PdlError(
                                "PDL lookup failed",
                                emptyList(),
                                listOf("hentIdenter"),
                                PdlErrorExtension("invalid", "ExecutionAborted"),
                            ),
                        ),
                )
            val event = failureEvent(error)
            event["exception_type"].asText() shouldBe "PdlRequestFailedException"
            event["pdl_operation"].asText() shouldBe "aktorid_fetch"
            event["pdl_errors"][0]["message"].asText() shouldBe "PDL lookup failed"
            event["upstream_status"] shouldBe null
            event.toString().contains("PRIVATE_") shouldBe false
            event.toString().contains("12345678910") shouldBe false
        }
    })

private fun failureEvent(error: PdlRequestFailedException): tools.jackson.databind.JsonNode {
    val service = mockk<DialogmoteStatusService>()
    val message = mockk<KDialogmoteStatusEndring>()
    every { message.getDialogmoteUuid() } returns "meeting-uuid"
    every { service.receiveKDialogmoteStatusendring(message) } throws error
    val logs =
        captureApplicationLogs {
            DialogmoteStatusendringListener(service).dialogmoteStatusEndringListener(
                ConsumerRecord("dialogmote-status", 0, 0, "key", message),
                mockk<Acknowledgment>(relaxed = true),
            )
        }
    val errors = logs.filter { it["level"].asText() == "ERROR" }
    errors.size shouldBe 1
    return errors.single()
}
