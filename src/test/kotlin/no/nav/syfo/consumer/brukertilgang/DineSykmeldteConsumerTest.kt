package no.nav.syfo.consumer.brukertilgang

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.syfo.api.auth.tokenX.TokenXUtil.TokenXIssuer
import no.nav.syfo.api.exception.ControllerExceptionHandler
import no.nav.syfo.consumer.tokenx.tokendings.TokenDingsConsumer
import no.nav.syfo.metric.Metric
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testhelper.UserConstants.NARMESTE_LEDER_ID
import no.nav.syfo.testhelper.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.testhelper.captureApplicationLogs
import no.nav.syfo.util.APP_CONSUMER_ID
import no.nav.syfo.util.NAV_CONSUMER_ID_HEADER
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.context.request.WebRequest
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.net.UnknownHostException
import java.time.Duration
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference

class DineSykmeldteConsumerTest :
    FunSpec({
        test("HTTP, DNS, timeout and invalid JSON produce one diagnostic event at the terminal handler") {
            val cases =
                listOf(
                    Triple(Mono.just(clientResponse(HttpStatus.INTERNAL_SERVER_ERROR, "PRIVATE_RESPONSE_CANARY")), "http", 500),
                    Triple(Mono.error<ClientResponse>(UnknownHostException("PRIVATE_HOST_CANARY")), "dns", null),
                    Triple(Mono.error<ClientResponse>(TimeoutException("PRIVATE_TIMEOUT_CANARY")), "timeout", null),
                    Triple(
                        Mono.just(clientResponse(HttpStatus.OK, "{\"fnr\":\"12345678910\",broken PRIVATE_JSON_CANARY")),
                        "invalid_response",
                        null,
                    ),
                )
            cases.forEach { (response, kind, status) ->
                val fixture = fixture(response)
                val logs =
                    captureApplicationLogs {
                        val exception =
                            shouldThrow<DineSykmeldteRequestException> {
                                fixture.consumer.getSykmeldt(NARMESTE_LEDER_ID)
                            }
                        ControllerExceptionHandler(fixture.metric)
                            .handleException(
                                exception,
                                mockk<WebRequest>(relaxed = true),
                            ).statusCode shouldBe HttpStatus.BAD_GATEWAY
                    }
                logs.size shouldBe 1
                val event = logs.single()
                event["level"].asText() shouldBe "ERROR"
                event["event_type"].asText() shouldBe "sykmeldt_lookup_failed"
                event["upstream"].asText() shouldBe "dinesykmeldte-backend"
                event["operation"].asText() shouldBe "sykmeldt_fetch"
                event["failure_kind"].asText() shouldBe kind
                event["failure_stage"].asText() shouldBe if (kind == "invalid_response") "response_decode" else "upstream_request"
                event["upstream_status"]?.asInt() shouldBe status
                listOf("PRIVATE_", "12345678910", NARMESTE_LEDER_ID.toString(), INCOMING_TOKEN, EXCHANGED_TOKEN).forEach {
                    event.toString().contains(it) shouldBe false
                }
                if (kind == "dns") event["cause_type"].asText() shouldBe "UnknownHostException"
            }
        }

        test("token exchange failure is attributed to tokenx before the upstream request") {
            val fixture = fixture(Mono.empty())
            val failure = HttpClientErrorException(HttpStatus.UNAUTHORIZED, "PRIVATE_TOKEN_CANARY")
            every { fixture.tokenDingsConsumer.exchangeToken(any(), any()) } throws failure
            val logs =
                captureApplicationLogs {
                    val exception =
                        shouldThrow<DineSykmeldteRequestException> {
                            fixture.consumer.getSykmeldt(NARMESTE_LEDER_ID)
                        }
                    exception.cause shouldBe failure
                    ControllerExceptionHandler(fixture.metric).handleException(exception, mockk<WebRequest>(relaxed = true))
                }
            logs.size shouldBe 1
            logs.single()["upstream"].asText() shouldBe "tokenx"
            logs.single()["failure_stage"].asText() shouldBe "token_exchange"
            logs.single()["upstream_status"].asInt() shouldBe 401
            logs.single().toString().contains("PRIVATE_TOKEN_CANARY") shouldBe false
            fixture.request.get() shouldBe null
        }

        test("cancellation propagates without a terminal error event") {
            val cancelled = CancellationException("cancelled")
            val fixture = fixture(Mono.error(cancelled))
            val logs =
                captureApplicationLogs {
                    shouldThrow<CancellationException> {
                        fixture.consumer.getSykmeldt(NARMESTE_LEDER_ID)
                    } shouldBe cancelled
                    shouldThrow<CancellationException> {
                        ControllerExceptionHandler(fixture.metric).handleException(cancelled, mockk<WebRequest>(relaxed = true))
                    } shouldBe cancelled
                }
            logs shouldBe emptyList()
        }

        test("veksler token og leser tilgangsfeltene uten å bruke aktivSykmelding") {
            val response =
                clientResponse(
                    HttpStatus.OK,
                    """
                    {
                      "narmestelederId": "$NARMESTE_LEDER_ID",
                      "orgnummer": "$VIRKSOMHETSNUMMER",
                      "fnr": "$ARBEIDSTAKER_FNR",
                      "navn": "Testperson",
                      "sykmeldinger": [],
                      "aktivSykmelding": false
                    }
                    """.trimIndent(),
                )
            val fixture = fixture(Mono.just(response))

            fixture.consumer.getSykmeldt(NARMESTE_LEDER_ID) shouldBe
                DineSykmeldteResponse(
                    fnr = ARBEIDSTAKER_FNR,
                    orgnummer = VIRKSOMHETSNUMMER,
                )

            val request = fixture.request.get()
            request.url().toString() shouldBe
                "$BASE_URL/api/v2/dinesykmeldte/$NARMESTE_LEDER_ID"
            request.headers().getFirst(HttpHeaders.AUTHORIZATION) shouldBe "Bearer $EXCHANGED_TOKEN"
            request.headers().getFirst(NAV_CONSUMER_ID_HEADER) shouldBe APP_CONSUMER_ID
            verify(exactly = 1) {
                fixture.tokenDingsConsumer.exchangeToken(INCOMING_TOKEN, TARGET_APP)
                fixture.metric.countOutgoingReponses(
                    DineSykmeldteConsumer.METRIC_CALL_DINE_SYKMELDTE,
                    HttpStatus.OK.value(),
                )
            }
        }

        test("tolker 404 som manglende tilgang") {
            val fixture = fixture(Mono.just(clientResponse(HttpStatus.NOT_FOUND)))

            fixture.consumer.getSykmeldt(NARMESTE_LEDER_ID) shouldBe null

            verify(exactly = 1) {
                fixture.metric.countOutgoingReponses(
                    DineSykmeldteConsumer.METRIC_CALL_DINE_SYKMELDTE,
                    HttpStatus.NOT_FOUND.value(),
                )
            }
        }

        test("mapper 401 fra Dine sykmeldte som teknisk feil") {
            val fixture = fixture(Mono.just(clientResponse(HttpStatus.UNAUTHORIZED)))

            shouldThrow<DineSykmeldteRequestException> {
                fixture.consumer.getSykmeldt(NARMESTE_LEDER_ID)
            }

            verify(exactly = 1) {
                fixture.metric.countOutgoingReponses(
                    DineSykmeldteConsumer.METRIC_CALL_DINE_SYKMELDTE,
                    HttpStatus.UNAUTHORIZED.value(),
                )
            }
        }

        test("propagerer andre downstream-feil som teknisk feil") {
            val fixture = fixture(Mono.just(clientResponse(HttpStatus.INTERNAL_SERVER_ERROR)))

            shouldThrow<DineSykmeldteRequestException> {
                fixture.consumer.getSykmeldt(NARMESTE_LEDER_ID)
            }

            verify(exactly = 1) {
                fixture.metric.countOutgoingReponses(
                    DineSykmeldteConsumer.METRIC_CALL_DINE_SYKMELDTE,
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                )
            }
        }

        test("bevarer årsaken til tekniske feil uten å endre den offentlige meldingen") {
            val fixture =
                fixture(
                    Mono.error(
                        IllegalStateException("Sensitive downstream details"),
                    ),
                )

            val exception =
                shouldThrow<DineSykmeldteRequestException> {
                    fixture.consumer.getSykmeldt(NARMESTE_LEDER_ID)
                }

            exception.message shouldBe "Request to dinesykmeldte-backend failed"
            exception.cause?.let { it is Exception } shouldBe true
        }

        test("mapper timeout fra Dine sykmeldte som sanert teknisk feil") {
            val fixture = fixture(Mono.never(), Duration.ZERO)

            val exception =
                shouldThrow<DineSykmeldteRequestException> {
                    fixture.consumer.getSykmeldt(NARMESTE_LEDER_ID)
                }

            exception.message shouldBe "Request to dinesykmeldte-backend failed"
            exception.cause?.let { it is Exception } shouldBe true
        }
    })

private data class DineSykmeldteConsumerFixture(
    val consumer: DineSykmeldteConsumer,
    val metric: Metric,
    val tokenDingsConsumer: TokenDingsConsumer,
    val request: AtomicReference<ClientRequest>,
)

private fun fixture(
    response: Mono<ClientResponse>,
    requestTimeout: Duration = Duration.ofSeconds(10),
): DineSykmeldteConsumerFixture {
    val request = AtomicReference<ClientRequest>()
    val exchangeFunction =
        ExchangeFunction { clientRequest ->
            request.set(clientRequest)
            response
        }
    val contextHolder = mockk<TokenValidationContextHolder>()
    val validationContext = mockk<TokenValidationContext>()
    val jwtToken = mockk<JwtToken>()
    val metric = mockk<Metric>(relaxed = true)
    val tokenDingsConsumer = mockk<TokenDingsConsumer>()

    every { contextHolder.getTokenValidationContext() } returns validationContext
    every { validationContext.getJwtToken(TokenXIssuer.TOKENX) } returns jwtToken
    every { jwtToken.encodedToken } returns INCOMING_TOKEN
    every { tokenDingsConsumer.exchangeToken(INCOMING_TOKEN, TARGET_APP) } returns EXCHANGED_TOKEN

    return DineSykmeldteConsumerFixture(
        consumer =
            DineSykmeldteConsumer(
                contextHolder = contextHolder,
                webClient = WebClient.builder().exchangeFunction(exchangeFunction).build(),
                metric = metric,
                tokenDingsConsumer = tokenDingsConsumer,
                baseUrl = BASE_URL,
                targetApp = TARGET_APP,
                requestTimeout = requestTimeout,
            ),
        metric = metric,
        tokenDingsConsumer = tokenDingsConsumer,
        request = request,
    )
}

private fun clientResponse(
    status: HttpStatus,
    body: String? = null,
): ClientResponse {
    val builder = ClientResponse.create(status)
    if (body != null) {
        builder
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(body)
    }
    return builder.build()
}

private const val BASE_URL = "http://dinesykmeldte-backend"
private const val TARGET_APP = "cluster:team-esyfo:dinesykmeldte-backend"
private const val INCOMING_TOKEN = "incoming-token"
private const val EXCHANGED_TOKEN = "exchanged-token"
