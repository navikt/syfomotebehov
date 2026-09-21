package no.nav.syfo.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.syfo.testhelper.captureApplicationLogs
import org.slf4j.LoggerFactory
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
