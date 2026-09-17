package no.nav.syfo.consumer.brukertilgang

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.api.auth.tokenX.TokenXUtil
import no.nav.syfo.consumer.tokenx.tokendings.TokenDingsConsumer
import no.nav.syfo.metric.Metric
import no.nav.syfo.util.APP_CONSUMER_ID
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.NAV_CONSUMER_ID_HEADER
import no.nav.syfo.util.bearerCredentials
import no.nav.syfo.util.createCallId
import org.slf4j.LoggerFactory
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
        try {
            val exchangedToken =
                tokenDingsConsumer.exchangeToken(
                    TokenXUtil.tokenFromTokenX(contextHolder),
                    targetApp,
                )

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
                        statusCode.value() == HttpStatus.UNAUTHORIZED.value() ->
                            Mono.error(
                                DineSykmeldteRequestException(
                                    "Unauthorized request to dinesykmeldte-backend",
                                ),
                            )
                        else -> {
                            LOG.error(
                                "Error requesting sykmeldt from dinesykmeldte-backend with status {} and callId {}",
                                statusCode.value(),
                                callId,
                            )
                            Mono.error(
                                DineSykmeldteRequestException(
                                    "Unexpected response from dinesykmeldte-backend: ${statusCode.value()}",
                                ),
                            )
                        }
                    }
                }.timeout(requestTimeout)
                .block()
        } catch (exception: DineSykmeldteRequestException) {
            throw exception
        } catch (exception: Exception) {
            LOG.error(
                "Technical error requesting sykmeldt from dinesykmeldte-backend with callId {}",
                callId,
            )
            throw DineSykmeldteRequestException("Request to dinesykmeldte-backend failed")
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(DineSykmeldteConsumer::class.java)
        const val DINE_SYKMELDTE_PATH = "/api/v2/dinesykmeldte/{narmesteLederId}"
        const val METRIC_CALL_DINE_SYKMELDTE = "call_dinesykmeldte_backend"
    }
}
