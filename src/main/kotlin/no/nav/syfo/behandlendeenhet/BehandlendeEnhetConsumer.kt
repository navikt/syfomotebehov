package no.nav.syfo.behandlendeenhet

import no.nav.syfo.config.CacheConfig
import no.nav.syfo.sts.StsConsumer
import no.nav.syfo.util.Metrikk
import no.nav.syfo.util.bearerCredentials
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate

@Service
class BehandlendeEnhetConsumer(
        private val metric: Metrikk,
        @Value("\${syfobehandlendeenhet.url}") private val baseUrl: String,
        private val stsConsumer: StsConsumer,
        private val template: RestTemplate
) {

    @Cacheable(cacheNames = [CacheConfig.CACHENAME_BEHANDLENDEENHET_FNR], key = "#fnr", condition = "#fnr != null")
    fun getBehandlendeEnhet(fnr: String): BehandlendeEnhet {
        metric.tellEndepunktKall(METRIC_CALL_BEHANDLENDEENHET)
        val bearer = stsConsumer.token()

        val request = HttpEntity<Any>(authorizationHeader(bearer))

        val url = getBehandlendeEnhetUrl(fnr)

        try {
            val response = template.exchange<BehandlendeEnhet>(
                    url,
                    HttpMethod.GET,
                    request,
                    object : ParameterizedTypeReference<BehandlendeEnhet>() {}
            )
            val responseBody = response.body

            metric.tellEndepunktKall(METRIC_CALL_BEHANDLENDEENHET_SUCCESS)

            return responseBody!!
        } catch (e: HttpClientErrorException) {
            metric.tellEndepunktKall(METRIC_CALL_BEHANDLENDEENHET_FAIL)
            throw e
        }
    }

    companion object {
        const val METRIC_CALL_BEHANDLENDEENHET = "call_behandlendeenhet"
        const val METRIC_CALL_BEHANDLENDEENHET_SUCCESS = "call_behandlendeenhet_success"
        const val METRIC_CALL_BEHANDLENDEENHET_FAIL = "call_behandlendeenhet_fail"
    }

    private fun getBehandlendeEnhetUrl(bruker: String): String {
        return "$baseUrl/api/$bruker"
    }

    private fun authorizationHeader(token: String): HttpHeaders {
        val credentials = bearerCredentials(token)
        val headers = HttpHeaders()
        headers.add(HttpHeaders.AUTHORIZATION, credentials)
        return headers
    }
}
