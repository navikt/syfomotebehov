package no.nav.syfo.consumer.brukertilgang

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.api.auth.tokenX.TokenXUtil
import no.nav.syfo.consumer.tokenx.tokendings.TokenDingsConsumer
import no.nav.syfo.metric.Metric
import no.nav.syfo.util.APP_CONSUMER_ID
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.NAV_CONSUMER_ID_HEADER
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.util.bearerCredentials
import no.nav.syfo.util.createCallId
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

@Service
class BrukertilgangConsumer(
    private val contextHolder: TokenValidationContextHolder,
    private val webClient: WebClient,
    private val metric: Metric,
    private val tokenDingsConsumer: TokenDingsConsumer,
    @Value("\${syfobrukertilgang.url}") private val baseUrl: String,
    @Value("\${syfobrukertilgang.client.id}") private val targetApp: String
) {
    fun hasAccessToAnsatt(ansattFnr: String): Boolean {
        val callId = createCallId()
        val exchangedToken = tokenDingsConsumer.exchangeToken(TokenXUtil.tokenFromTokenX(contextHolder), targetApp)

        val response = webClient
            .get()
            .uri("$baseUrl/api/v2/tilgang/ansatt")
            .header(HttpHeaders.AUTHORIZATION, bearerCredentials(exchangedToken))
            .header(NAV_PERSONIDENT_HEADER, ansattFnr)
            .header(NAV_CALL_ID_HEADER, callId)
            .header(NAV_CONSUMER_ID_HEADER, APP_CONSUMER_ID)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus({ obj: HttpStatusCode -> obj.value() == 401 }) { response ->
                metric.countOutgoingReponses(METRIC_CALL_BRUKERTILGANG, response.statusCode().value())
                Mono.error(RequestUnauthorizedException("Unauthorized request to get access to Ansatt from Syfobrukertilgang"))
            }
            .onStatus({ obj: HttpStatusCode -> obj.is4xxClientError }) { response ->
                logError(response, callId)
                Mono.error(RuntimeException("4xx"))
            }
            .onStatus({ obj: HttpStatusCode -> obj.is5xxServerError }) { response ->
                logError(response, callId)
                Mono.error(RuntimeException("5xx"))
            }
            .bodyToMono<Boolean>()
            .block()
        metric.countOutgoingReponses(METRIC_CALL_BRUKERTILGANG, 200)
        return response ?: false
    }

    fun logError(response: ClientResponse, callId: String) {
        metric.countOutgoingReponses(METRIC_CALL_BRUKERTILGANG, response.statusCode().value())
        LOG.error("Error requesting ansatt access from syfobrukertilgang with callId $callId")
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(BrukertilgangConsumer::class.java)
        const val METRIC_CALL_BRUKERTILGANG = "call_syfobrukertilgang"
    }
}
