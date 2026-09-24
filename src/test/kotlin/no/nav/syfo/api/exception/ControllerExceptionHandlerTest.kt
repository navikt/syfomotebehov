package no.nav.syfo.api.exception

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.ws.rs.ForbiddenException
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import no.nav.syfo.consumer.brukertilgang.DineSykmeldteRequestException
import no.nav.syfo.metric.Metric
import no.nav.syfo.testhelper.captureApplicationLogs
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.util.WebUtils
import java.util.concurrent.CancellationException

class ControllerExceptionHandlerTest :
    FunSpec({
        test("unexpected API error retains code locations without exception message or cause payload") {
            val handler = ControllerExceptionHandler(mockk<Metric>(relaxed = true))
            val error = IllegalStateException("PRIVATE_PERSON_12345678910", IllegalArgumentException("PRIVATE_PAYLOAD"))
            val logs =
                captureApplicationLogs {
                    handler.handleException(error, mockk<WebRequest>(relaxed = true)).statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
                }
            logs.size shouldBe 1
            val event = logs.single()
            event["event_type"].asText() shouldBe "api_request_failed"
            event["exception_type"].asText() shouldBe "IllegalStateException"
            event["cause_type"].asText() shouldBe "IllegalArgumentException"
            event["stack_trace"].asText().contains("ControllerExceptionHandlerTest") shouldBe true
            event.toString().contains("PRIVATE_") shouldBe false
            event.toString().contains("12345678910") shouldBe false
        }

        test("mapper autentiseringsfeil fra Dine sykmeldte til bad gateway") {
            val handler = ControllerExceptionHandler(mockk<Metric>(relaxed = true))

            val response =
                handler.handleException(
                    DineSykmeldteRequestException("Unauthorized request to dinesykmeldte-backend"),
                    mockk<WebRequest>(relaxed = true),
                )

            response.statusCode shouldBe HttpStatus.BAD_GATEWAY
        }

        test("bevarer innkommende TokenX-autentiseringsfeil som unauthorized") {
            val handler = ControllerExceptionHandler(mockk<Metric>(relaxed = true))
            val tokenException = mockk<JwtTokenUnauthorizedException>()
            every { tokenException.message } returns "Invalid TokenX token"
            every { tokenException.cause } returns null

            val logs =
                captureApplicationLogs {
                    handler
                        .handleException(
                            tokenException,
                            mockk<WebRequest>(relaxed = true),
                        ).statusCode shouldBe HttpStatus.UNAUTHORIZED
                }
            logs.single()["event_type"].asText() shouldBe "api_request_rejected"
            logs.single()["exception_type"].asText().isNotEmpty() shouldBe true
        }

        test("mapper ulesbar request-body til bad request") {
            val metric = mockk<Metric>(relaxed = true)
            val handler = ControllerExceptionHandler(metric)

            val logs =
                captureApplicationLogs {
                    handler
                        .handleException(
                            HttpMessageNotReadableException("Invalid UUID", mockk<HttpInputMessage>()),
                            mockk<WebRequest>(relaxed = true),
                        ).statusCode shouldBe HttpStatus.BAD_REQUEST
                }
            logs.single()["event_type"].asText() shouldBe "api_request_invalid"
            logs.single()["exception_type"].asText() shouldBe "HttpMessageNotReadableException"
            verify(exactly = 1) { metric.tellHttpKall(HttpStatus.BAD_REQUEST.value()) }
        }

        test("cancellation returns 500 with one WARN event without a stack trace or message") {
            val metric = mockk<Metric>(relaxed = true)
            val request = mockk<WebRequest>(relaxed = true)
            val error = IllegalStateException("PRIVATE_MESSAGE", CancellationException("PRIVATE_CANCELLED"))
            val logs =
                captureApplicationLogs {
                    ControllerExceptionHandler(metric)
                        .handleException(
                            error,
                            request,
                        ).statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
                }
            val event = logs.single()
            event["level"].asText() shouldBe "WARN"
            event["event_type"].asText() shouldBe "api_request_cancelled"
            event["exception_type"].asText() shouldBe "IllegalStateException"
            event["response_status"].asInt() shouldBe 500
            event["stack_trace"] shouldBe null
            event.toString().contains("PRIVATE_") shouldBe false
            verify(exactly = 1) { metric.tellHttpKall(500) }
            verify(exactly = 1) { request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, error, WebRequest.SCOPE_REQUEST) }
        }

        test("bad request and forbidden request have distinct sanitized WARN events") {
            val handler = ControllerExceptionHandler(mockk<Metric>(relaxed = true))
            val logs =
                captureApplicationLogs {
                    handler.handleException(IllegalArgumentException("PRIVATE_INPUT"), mockk<WebRequest>(relaxed = true))
                    handler.handleException(ForbiddenException("PRIVATE_ACCESS"), mockk<WebRequest>(relaxed = true))
                }
            logs.size shouldBe 2
            logs[0]["event_type"].asText() shouldBe "api_request_invalid"
            logs[0]["error_type"].asText() shouldBe "INVALID_INPUT"
            logs[0]["exception_type"].asText() shouldBe "IllegalArgumentException"
            logs[0]["response_status"].asInt() shouldBe 400
            logs[1]["event_type"].asText() shouldBe "api_request_rejected"
            logs[1]["rejection_reason"].asText() shouldBe "FORBIDDEN"
            logs[1]["exception_type"].asText() shouldBe "ForbiddenException"
            logs[1]["response_status"].asInt() shouldBe 403
            logs.forEach {
                it["stack_trace"] shouldBe null
                it.toString().contains("PRIVATE_") shouldBe false
            }
        }

        test("conflict keeps its exception type in the rejection event") {
            val logs =
                captureApplicationLogs {
                    ControllerExceptionHandler(mockk<Metric>(relaxed = true))
                        .handleException(ConflictException(), mockk<WebRequest>(relaxed = true))
                        .statusCode shouldBe HttpStatus.CONFLICT
                }
            logs.single()["exception_type"].asText() shouldBe "ConflictException"
            logs.single()["rejection_reason"].asText() shouldBe "STATE_CONFLICT"
            logs.single()["stack_trace"] shouldBe null
        }

        test("interrupted cause is logged without changing the thread interrupt flag") {
            val interruptedBefore = Thread.currentThread().isInterrupted
            val logs =
                captureApplicationLogs {
                    ControllerExceptionHandler(mockk<Metric>(relaxed = true))
                        .handleException(
                            IllegalStateException("PRIVATE_MESSAGE", InterruptedException("PRIVATE_CAUSE")),
                            mockk(relaxed = true),
                        ).statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
                }
            logs.single()["event_type"].asText() shouldBe "api_request_cancelled"
            Thread.currentThread().isInterrupted shouldBe interruptedBefore
        }

        test("mapper ugyldig path-parameter til sanitert bad request") {
            val metric = mockk<Metric>(relaxed = true)
            val handler = ControllerExceptionHandler(metric)

            val response =
                handler.handleException(
                    mockk<MethodArgumentTypeMismatchException>(relaxed = true),
                    mockk<WebRequest>(relaxed = true),
                )

            response.statusCode shouldBe HttpStatus.BAD_REQUEST
            response.body shouldBe ApiError(HttpStatus.BAD_REQUEST.value(), "Vi kunne ikke tolke inndataene")
            verify(exactly = 1) { metric.tellHttpKall(HttpStatus.BAD_REQUEST.value()) }
        }
    })
