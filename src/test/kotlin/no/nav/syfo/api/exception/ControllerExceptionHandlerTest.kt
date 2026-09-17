package no.nav.syfo.api.exception

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import no.nav.syfo.consumer.brukertilgang.DineSykmeldteRequestException
import no.nav.syfo.metric.Metric
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

class ControllerExceptionHandlerTest :
    FunSpec({
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
            verify(exactly = 1) { metric.tellHttpKall(HttpStatus.BAD_REQUEST.value()) }
        }
    })
