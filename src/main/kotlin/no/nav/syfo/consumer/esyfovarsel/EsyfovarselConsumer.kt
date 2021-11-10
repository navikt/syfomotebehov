package no.nav.syfo.consumer.esyfovarsel

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.api.auth.OIDCIssuer
import no.nav.syfo.api.auth.OIDCUtil
import no.nav.syfo.consumer.brukertilgang.RequestUnauthorizedException
import no.nav.syfo.metric.Metric
import no.nav.syfo.util.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

@Service
class EsyfovarselConsumer(
    private val contextHolder: TokenValidationContextHolder,
    private val webClient: WebClient,
    private val metric: Metric,
    @Value("\${esyfovarselapi.url}") private val baseUrl: String
) {
    fun varsel39Sent(aktorId: String): Boolean {
        val callId = createCallId()
        val response = webClient
            .get()
            .uri(esyfovarselURL(aktorId))
            .header(HttpHeaders.AUTHORIZATION, bearerCredentials(OIDCUtil.tokenFraOIDC(contextHolder, OIDCIssuer.EKSTERN)))
            .header(NAV_CALL_ID_HEADER, callId)
            .header(NAV_CONSUMER_ID_HEADER, APP_CONSUMER_ID)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus({ obj: HttpStatus -> obj.value() == 401 }) { response ->
                metric.countOutgoingReponses(METRIC_CALL_ESYFOVARSEL, response.rawStatusCode())
                Mono.error(RequestUnauthorizedException("Unauthorized request to get varsel from esyfovarsel"))
            }
            .onStatus({ obj: HttpStatus -> obj.is4xxClientError }) { response ->
                logError(response, callId)
                Mono.error(RuntimeException("4xx"))
            }
            .onStatus({ obj: HttpStatus -> obj.is5xxServerError }) { response ->
                logError(response, callId)
                Mono.error(RuntimeException("5xx"))
            }
            .bodyToMono<Boolean>()
            .block()
        metric.countOutgoingReponses(METRIC_CALL_ESYFOVARSEL, 200)
        return response ?: false
    }

    fun logError(response: ClientResponse, callId: String) {
        metric.countOutgoingReponses(METRIC_CALL_ESYFOVARSEL, response.rawStatusCode())
        LOG.error("Error requesting esyfovarsel with callId $callId")
    }

    private fun esyfovarselURL(aktorId: String) = "$baseUrl/39ukersvarsel/$aktorId"

    companion object {
        private val LOG = LoggerFactory.getLogger(EsyfovarselConsumer::class.java)

        const val METRIC_CALL_ESYFOVARSEL = "call_esyfovarsel"
    }
}
