package no.nav.syfo.consumer.brukertilgang

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.syfo.api.auth.tokenX.TokenXUtil.TokenXIssuer
import no.nav.syfo.consumer.tokenx.tokendings.TokenDingsConsumer
import no.nav.syfo.metric.Metric
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testhelper.UserConstants.NARMESTE_LEDER_ID
import no.nav.syfo.testhelper.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.util.APP_CONSUMER_ID
import no.nav.syfo.util.NAV_CONSUMER_ID_HEADER
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference

class DineSykmeldteConsumerTest :
    DescribeSpec({
        describe("DineSykmeldteConsumer") {
            it("veksler token og leser tilgangsfeltene uten å bruke aktivSykmelding") {
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

            it("tolker 404 som manglende tilgang") {
                val fixture = fixture(Mono.just(clientResponse(HttpStatus.NOT_FOUND)))

                fixture.consumer.getSykmeldt(NARMESTE_LEDER_ID) shouldBe null

                verify(exactly = 1) {
                    fixture.metric.countOutgoingReponses(
                        DineSykmeldteConsumer.METRIC_CALL_DINE_SYKMELDTE,
                        HttpStatus.NOT_FOUND.value(),
                    )
                }
            }

            it("mapper 401 fra Dine sykmeldte som teknisk feil") {
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

            it("propagerer andre downstream-feil som teknisk feil") {
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

            it("saniterer tekniske feil uten HTTP-respons") {
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
                exception.cause shouldBe null
            }

            it("mapper timeout fra Dine sykmeldte som sanert teknisk feil") {
                val fixture = fixture(Mono.never(), Duration.ZERO)

                val exception =
                    shouldThrow<DineSykmeldteRequestException> {
                        fixture.consumer.getSykmeldt(NARMESTE_LEDER_ID)
                    }

                exception.message shouldBe "Request to dinesykmeldte-backend failed"
                exception.cause shouldBe null
            }
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
