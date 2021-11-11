package no.nav.syfo.consumer.brukertilgang

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.api.auth.OIDCIssuer
import no.nav.syfo.api.auth.OIDCUtil
import no.nav.syfo.metric.Metric
import no.nav.syfo.util.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.*
import reactor.core.publisher.Mono

@Service
class BrukertilgangConsumer(
    private val contextHolder: TokenValidationContextHolder,
    private val webClient: WebClient,
    private val metric: Metric,
    @Value("\${syfobrukertilgang.url}") private val baseUrl: String
) {
    fun hasAccessToAnsatt(ansattFnr: String): Boolean {
        val callId = createCallId()
        val response = webClient
            .get()
            .uri(arbeidstakerUrl(ansattFnr))
            .header(HttpHeaders.AUTHORIZATION, bearerCredentials(OIDCUtil.tokenFraOIDC(contextHolder, OIDCIssuer.EKSTERN)))
            .header(NAV_CALL_ID_HEADER, callId)
            .header(NAV_CONSUMER_ID_HEADER, APP_CONSUMER_ID)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus({ obj: HttpStatus -> obj.value() == 401 }) { response ->
                metric.countOutgoingReponses(METRIC_CALL_BRUKERTILGANG, response.rawStatusCode())
                Mono.error(RequestUnauthorizedException("Unauthorized request to get access to Ansatt from Syfobrukertilgang"))
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
        metric.countOutgoingReponses(METRIC_CALL_BRUKERTILGANG, 200)
        return response ?: false
    }

    fun logError(response: ClientResponse, callId: String) {
        metric.countOutgoingReponses(METRIC_CALL_BRUKERTILGANG, response.rawStatusCode())
        LOG.error("Error requesting ansatt access from syfobrukertilgang with callId $callId")
    }

    private fun arbeidstakerUrl(ansattFnr: String) = "$baseUrl/api/v1/tilgang/ansatt/$ansattFnr"

    companion object {
        private val LOG = LoggerFactory.getLogger(BrukertilgangConsumer::class.java)

        const val METRIC_CALL_BRUKERTILGANG = "call_syfobrukertilgang"
    }
}
