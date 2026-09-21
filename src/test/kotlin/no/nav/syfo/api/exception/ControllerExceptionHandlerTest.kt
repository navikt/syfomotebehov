package no.nav.syfo.api.exception

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import no.nav.syfo.consumer.brukertilgang.DineSykmeldteRequestException
import no.nav.syfo.metric.Metric
import no.nav.syfo.testhelper.captureApplicationLogs
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

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

            val response =
                handler.handleException(
                    tokenException,
                    mockk<WebRequest>(relaxed = true),
                )

            response.statusCode shouldBe HttpStatus.UNAUTHORIZED
        }

        test("mapper ulesbar request-body til bad request") {
            val metric = mockk<Metric>(relaxed = true)
            val handler = ControllerExceptionHandler(metric)

            val response =
                handler.handleException(
                    HttpMessageNotReadableException("Invalid UUID", mockk<HttpInputMessage>()),
                    mockk<WebRequest>(relaxed = true),
                )

            response.statusCode shouldBe HttpStatus.BAD_REQUEST
            verify(exactly = 1) { metric.tellHttpKall(HttpStatus.BAD_REQUEST.value()) }
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
