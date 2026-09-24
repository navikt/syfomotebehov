package no.nav.syfo.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.syfo.testhelper.captureApplicationLogs
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.RecordDeserializationException
import org.slf4j.LoggerFactory
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.kafka.support.serializer.DeserializationException
import org.springframework.web.client.RestClientException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.CancellationException
import javax.net.ssl.SSLException

class FailureDiagnosticsTest :
    FunSpec({
        test("nested transport failures preserve the specific cause category") {
            listOf(
                UnknownHostException("PRIVATE_HOST") to FailureKind.DNS,
                SocketTimeoutException("PRIVATE_URL") to FailureKind.TIMEOUT,
                ConnectException("PRIVATE_HOST") to FailureKind.CONNECTION,
                SSLException("PRIVATE_CERTIFICATE") to FailureKind.TLS,
                org.apache.kafka.common.errors
                    .TimeoutException("PRIVATE_KAFKA") to FailureKind.TIMEOUT,
            ).forEach { (cause, expected) ->
                val wrapper = RuntimeException("PRIVATE_WRAPPER", cause)
                wrapper.failureKind() shouldBe expected
                val logs =
                    captureApplicationLogs {
                        LoggerFactory
                            .getLogger("no.nav.syfo.diagnostics")
                            .atError()
                            .withFailureDiagnostics(wrapper)
                            .log("Connection failed")
                    }
                logs.single().toString().contains("PRIVATE_") shouldBe false
                logs.single()["cause_type"].asString() shouldBe cause.javaClass.simpleName
            }
        }

        test("nested Kafka deserialization failures classify as invalid responses") {
            listOf(
                RecordDeserializationException(
                    TopicPartition("test-topic", 0),
                    0L,
                    "fake record decode failure",
                    IllegalArgumentException("fake record"),
                ),
                DeserializationException(
                    "fake value decode failure",
                    byteArrayOf(1, 2, 3),
                    false,
                    IllegalArgumentException("fake value"),
                ),
            ).forEach { cause ->
                RuntimeException("fake wrapper", cause).failureKind() shouldBe FailureKind.INVALID_RESPONSE
            }
        }

        test("database diagnostics preserve standard SQL state without logging SQL or submitted data") {
            val error =
                org.springframework.dao.DataIntegrityViolationException(
                    "PRIVATE_SUBMITTED_FORM",
                    java.sql.SQLException("PRIVATE_SQL", "23505"),
                )
            val logs =
                captureApplicationLogs {
                    LoggerFactory
                        .getLogger("no.nav.syfo.diagnostics")
                        .atError()
                        .withFailureDiagnostics(error)
                        .log("Could not save the meeting need")
                }
            logs.single()["exception_type"].asString() shouldBe "DataIntegrityViolationException"
            logs.single()["sql_state"].asString() shouldBe "23505"
            logs.single().toString().contains("PRIVATE_") shouldBe false
        }

        test("unlisted exception types survive the sanitized cause chain and invalid SQL state is omitted") {
            val error = RuntimeException("PRIVATE_MESSAGE", java.sql.SQLException("PRIVATE_SQL", "PRIVATE_STATE"))
            val event =
                captureApplicationLogs {
                    LoggerFactory
                        .getLogger("no.nav.syfo.diagnostics")
                        .atError()
                        .withFailureDiagnostics(error)
                        .log("Failure")
                }.single()
            event["exception_type"].asText() shouldBe "RuntimeException"
            event["cause_type"].asText() shouldBe "SQLException"
            event["stack_trace"].asText().contains("RuntimeException") shouldBe true
            event["sql_state"] shouldBe null
            event.toString().contains("PRIVATE_") shouldBe false
        }

        test("cause cycles terminate and cancellation is rethrown") {
            val first = IllegalStateException("first")
            val second = IllegalStateException("second", first)
            first.initCause(second)
            first.failureKind() shouldBe FailureKind.UNKNOWN
            val cancelled = CancellationException("cancelled")
            shouldThrow<CancellationException> {
                RuntimeException(cancelled).rethrowIfCancelled()
            } shouldBe cancelled
        }

        test("Jackson 3 response decode errors classify as invalid responses through RestTemplate wrappers") {
            val jackson =
                tools.jackson.core.exc.JacksonIOException
                    .construct(java.io.IOException("PRIVATE_JSON"))
            val unreadable = HttpMessageNotReadableException("PRIVATE_BODY", jackson, io.mockk.mockk())
            val failure = RestClientException("PRIVATE_REQUEST", unreadable)
            failure.failureKind() shouldBe FailureKind.INVALID_RESPONSE
            val event =
                captureApplicationLogs {
                    LoggerFactory
                        .getLogger("no.nav.syfo.diagnostics")
                        .atError()
                        .withFailureDiagnostics(failure)
                        .log("Invalid response")
                }.single()
            event["cause_type"].asText() shouldBe "IOException"
            event["stack_trace"].asText().contains("JacksonIOException") shouldBe true
            event.toString().contains("PRIVATE_") shouldBe false
        }

        test("nested unlisted types keep their names without leaking exception messages") {
            val error = NoSuchElementException("PRIVATE_CAUSE")
            val event =
                captureApplicationLogs {
                    LoggerFactory
                        .getLogger("no.nav.syfo.diagnostics")
                        .atError()
                        .withFailureDiagnostics(RuntimeException("PRIVATE_WRAPPER", error))
                        .log("Failure")
                }.single()
            event["exception_type"].asText() shouldBe "RuntimeException"
            event["cause_type"].asText() shouldBe "NoSuchElementException"
            event["stack_trace"].asText().contains("NoSuchElementException") shouldBe true
            event.toString().contains("PRIVATE_") shouldBe false
        }

        test("interruption is preserved rather than reported as an upstream failure") {
            val interrupted = InterruptedException("shutdown")
            try {
                shouldThrow<InterruptedException> {
                    RuntimeException(interrupted).rethrowIfCancelled()
                } shouldBe interrupted
                Thread.currentThread().isInterrupted shouldBe true
            } finally {
                Thread.interrupted()
            }
        }
    })
