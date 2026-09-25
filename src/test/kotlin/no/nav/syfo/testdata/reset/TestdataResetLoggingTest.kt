package no.nav.syfo.testdata.reset

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.syfo.consumer.pdl.IPdlConsumer
import no.nav.syfo.consumer.pdl.PdlError
import no.nav.syfo.consumer.pdl.PdlErrorExtension
import no.nav.syfo.consumer.pdl.PdlRequestFailedException
import no.nav.syfo.oppfolgingstilfelle.kafka.TestdataResetListener
import no.nav.syfo.testhelper.captureApplicationLogs
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.support.Acknowledgment
import java.util.concurrent.CancellationException

class TestdataResetLoggingTest :
    FunSpec({
        test("a PDL failure during reset keeps its explanation in one log and does not acknowledge the message") {
            val pdl = mockk<IPdlConsumer>()
            every { pdl.aktorid(any()) } throws
                PdlRequestFailedException(
                    operation = "aktorid_fetch",
                    pdlErrors =
                        listOf(
                            PdlError(
                                "PDL access denied",
                                emptyList(),
                                listOf("hentIdenter"),
                                PdlErrorExtension("unauthorized", "ExecutionAborted"),
                            ),
                        ),
                )
            val service = TestdataResetService(mockk(), mockk(), mockk(), mockk(), pdl)
            val acknowledgment = mockk<Acknowledgment>(relaxed = true)
            val logs =
                captureApplicationLogs {
                    TestdataResetListener(service).testdataResetListener(
                        ConsumerRecord("testdata-reset", 0, 0, "key", "12345678910"),
                        acknowledgment,
                    )
                }
            val event = logs.single()
            event["event_type"].asString() shouldBe "pdl_lookup_failed"
            event["upstream"].asString() shouldBe "pdl"
            event["pdl_errors"][0]["message"].asString() shouldBe "PDL access denied"
            event.toString().contains("12345678910") shouldBe false
            verify(exactly = 0) { acknowledgment.acknowledge() }
        }

        test("cancellation during a PDL lookup is propagated without a failure log") {
            val cancelled = CancellationException("shutdown")
            val service = mockk<TestdataResetService>()
            every { service.resetTestdata(any()) } throws PdlRequestFailedException(cause = cancelled)
            val acknowledgment = mockk<Acknowledgment>(relaxed = true)
            val logs =
                captureApplicationLogs {
                    shouldThrow<CancellationException> {
                        TestdataResetListener(service).testdataResetListener(
                            ConsumerRecord("testdata-reset", 0, 0, "key", "12345678910"),
                            acknowledgment,
                        )
                    } shouldBe cancelled
                }
            logs.size shouldBe 0
            verify(exactly = 0) { acknowledgment.acknowledge() }
        }
    })
