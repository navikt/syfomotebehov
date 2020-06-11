package no.nav.syfo.consumer.aktorregister

import no.nav.syfo.cache.CacheConfig
import no.nav.syfo.consumer.aktorregister.domain.*
import no.nav.syfo.consumer.sts.StsConsumer
import no.nav.syfo.metric.Metric
import no.nav.syfo.util.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import javax.inject.Inject

@Service
class AktorregisterConsumer @Inject constructor(
    @Value("\${aktorregister.rest.url}") private val baseUrl: String,
    private val metric: Metric,
    private val restTemplate: RestTemplate,
    private val stsConsumer: StsConsumer
) {
    private val responseType: ParameterizedTypeReference<Map<String, IdentinfoListe>> = object : ParameterizedTypeReference<Map<String, IdentinfoListe>>() {}

    @Cacheable(value = [CacheConfig.CACHENAME_AKTOR_ID], key = "#fnr", condition = "#fnr != null")
    fun getAktorIdForFodselsnummer(fnr: Fodselsnummer): String {
        val response = getIdentFromAktorregister(fnr.value, IdentType.AktoerId)
        return currentIdentFromAktorregisterResponse(response, fnr.value, IdentType.AktoerId)
    }

    @Cacheable(value = [CacheConfig.CACHENAME_AKTOR_FNR], key = "#aktorId", condition = "#aktorId != null")
    fun getFnrForAktorId(aktorId: AktorId): String {
        val response = getIdentFromAktorregister(aktorId.value, IdentType.NorskIdent)
        return currentIdentFromAktorregisterResponse(response, aktorId.value, IdentType.NorskIdent)
    }

    private fun getIdentFromAktorregister(ident: String, identType: IdentType): Map<String, IdentinfoListe> {
        val entity = entity(ident)
        val uriString = UriComponentsBuilder.fromHttpUrl("$baseUrl/identer").queryParam("gjeldende", true).toUriString()
        try {
            val response = restTemplate.exchange(
                uriString,
                HttpMethod.GET,
                entity,
                responseType
            )
            val responseBody = response.body!!
            metric.tellHendelse("call_aktorregister_success")
            return responseBody
        } catch (e: RestClientResponseException) {
            metric.tellHendelse("call_aktorregister_fail")
            val message = "Call to get Ident from Aktorregister failed with status HTTP-${e.rawStatusCode} for IdentType=${identType.name}"
            LOG.error(message)
            throw e
        }
    }

    private fun entity(ident: String): HttpEntity<String> {
        val stsToken = stsConsumer.token()
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers[HttpHeaders.AUTHORIZATION] = bearerCredentials(stsToken)
        headers[NAV_CONSUMER_ID_HEADER] = APP_CONSUMER_ID
        headers[NAV_CALL_ID_HEADER] = createCallId()
        headers[NAV_PERSONIDENTER_HEADER] = ident
        return HttpEntity(headers)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(AktorregisterConsumer::class.java)
    }
}

enum class IdentType {
    AktoerId, NorskIdent
}
