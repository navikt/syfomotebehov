package no.nav.syfo.consumer.behandlendeenhet

import no.nav.syfo.cache.CacheConfig
import no.nav.syfo.consumer.azuread.v2.AzureAdV2TokenConsumer
import no.nav.syfo.metric.Metric
import no.nav.syfo.util.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate

@Service
class BehandlendeEnhetConsumer(
    private val azureAdV2TokenConsumer: AzureAdV2TokenConsumer,
    private val metric: Metric,
    @Value("\${syfobehandlendeenhet.client.id}") private val syfobehandlendeenhetClientId: String,
    @Value("\${syfobehandlendeenhet.url}") private val baseUrl: String,
    @Qualifier("restTemplateWithProxy") private val template: RestTemplate
) {

    @Cacheable(cacheNames = [CacheConfig.CACHENAME_BEHANDLENDEENHET_FNR], key = "#fnr", condition = "#fnr != null")
    fun getBehandlendeEnhet(fnr: String, callId: String?): BehandlendeEnhet {
        val bearer = azureAdV2TokenConsumer.getSystemToken(
            scopeClientId = syfobehandlendeenhetClientId
        )

        val httpEntity = entity(callId, bearer, fnr)
        try {
            val response = template.exchange<BehandlendeEnhet>(
                "$baseUrl$BEHANDLENDEENHET_PATH",
                HttpMethod.GET,
                httpEntity,
                BehandlendeEnhet::class.java
            )
            val responseBody = response.body!!
            metric.countOutgoingReponses(METRIC_CALL_BEHANDLENDEENHET, response.statusCodeValue)
            return responseBody
        } catch (e: RestClientResponseException) {
            LOG.error("Error requesting BehandlendeEnhet from syfobehandlendeenhet with callId ${httpEntity.headers[NAV_CALL_ID_HEADER]}: ", e)
            metric.countOutgoingReponses(METRIC_CALL_BEHANDLENDEENHET, e.rawStatusCode)
            throw e
        }
    }

    private fun entity(callId: String?, token: String, fnr: String): HttpEntity<String> {
        val credentials = bearerCredentials(token)
        val headers = HttpHeaders()
        headers[HttpHeaders.AUTHORIZATION] = credentials
        headers[NAV_CALL_ID_HEADER] = getOrCreateCallId(callId)
        headers[NAV_CONSUMER_ID_HEADER] = APP_CONSUMER_ID
        headers[NAV_PERSONIDENT_HEADER] = fnr
        return HttpEntity(headers)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(BehandlendeEnhetConsumer::class.java)

        const val BEHANDLENDEENHET_PATH = "/api/system/v2/personident"

        const val METRIC_CALL_BEHANDLENDEENHET = "call_behandlendeenhet"
    }
}
