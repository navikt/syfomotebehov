package no.nav.syfo.consumer.brukertilgang

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.api.auth.tokenX.TokenXUtil
import no.nav.syfo.consumer.tokenx.tokendings.TokenDingsConsumer
import no.nav.syfo.metric.Metric
import no.nav.syfo.util.APP_CONSUMER_ID
import no.nav.syfo.util.FailureKind
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.NAV_CONSUMER_ID_HEADER
import no.nav.syfo.util.bearerCredentials
import no.nav.syfo.util.createCallId
import no.nav.syfo.util.failureKind
import no.nav.syfo.util.rethrowIfCancelled
import no.nav.syfo.util.upstreamStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.UUID

@Profile("!local")
@Service
class DineSykmeldteConsumer(
    private val contextHolder: TokenValidationContextHolder,
    private val webClient: WebClient,
    private val metric: Metric,
    private val tokenDingsConsumer: TokenDingsConsumer,
    @Value("\${dinesykmeldte.url}") private val baseUrl: String,
    @Value("\${dinesykmeldte.client.id}") private val targetApp: String,
    @Value("\${dinesykmeldte.timeout:10s}") private val requestTimeout: Duration,
) : IDineSykmeldteConsumer {
    override fun getSykmeldt(narmesteLederId: UUID): DineSykmeldteResponse? {
        val callId = createCallId()
        var stage = SykmeldtFailureStage.TOKEN_EXCHANGE
        try {
            val exchangedToken =
                tokenDingsConsumer.exchangeToken(
                    TokenXUtil.tokenFromTokenX(contextHolder),
                    targetApp,
                )

            stage = SykmeldtFailureStage.UPSTREAM_REQUEST
            return webClient
                .get()
                .uri("$baseUrl$DINE_SYKMELDTE_PATH", narmesteLederId)
                .header(HttpHeaders.AUTHORIZATION, bearerCredentials(exchangedToken))
                .header(NAV_CALL_ID_HEADER, callId)
                .header(NAV_CONSUMER_ID_HEADER, APP_CONSUMER_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchangeToMono { response ->
                    val statusCode = response.statusCode()
                    metric.countOutgoingReponses(METRIC_CALL_DINE_SYKMELDTE, statusCode.value())
                    when {
                        statusCode.is2xxSuccessful -> response.bodyToMono<DineSykmeldteResponse>()
                        statusCode.value() == HttpStatus.NOT_FOUND.value() -> Mono.empty()
                        else ->
                            Mono.error(
                                DineSykmeldteRequestException(
                                    "Unexpected response from dinesykmeldte-backend",
                                    upstreamStatus = statusCode.value(),
                                ),
                            )
                    }
                }.timeout(requestTimeout)
                .block()
        } catch (exception: DineSykmeldteRequestException) {
            throw exception
        } catch (exception: Exception) {
            exception.rethrowIfCancelled()
            throw DineSykmeldteRequestException(
                "Request to dinesykmeldte-backend failed",
                cause = exception,
                stage =
                    if (stage == SykmeldtFailureStage.UPSTREAM_REQUEST && exception.failureKind() == FailureKind.INVALID_RESPONSE) {
                        SykmeldtFailureStage.RESPONSE_DECODE
                    } else {
                        stage
                    },
                upstreamStatus = exception.upstreamStatus(),
            )
        }
    }

    companion object {
        const val DINE_SYKMELDTE_PATH = "/api/v2/dinesykmeldte/{narmesteLederId}"
        const val METRIC_CALL_DINE_SYKMELDTE = "call_dinesykmeldte_backend"
    }
}
