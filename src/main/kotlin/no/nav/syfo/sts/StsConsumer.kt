package no.nav.syfo.sts

import no.nav.syfo.metric.Metric
import no.nav.syfo.util.basicCredentials
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import org.springframework.web.util.UriComponentsBuilder.fromHttpUrl
import java.time.LocalDateTime

@Service
class StsConsumer(
        private val metric: Metric,
        @Value("\${security.token.service.rest.url}") private val baseUrl: String,
        @Value("\${srv.username}") private val username: String,
        @Value("\${srv.password}") private val password: String,
        private val template: RestTemplate
) {
    private val getStsTokenUriTemplate: UriComponentsBuilder = fromHttpUrl(getStsTokenUrl())

    private var cachedOidcToken: STSToken? = null

    fun token(): String {
        if (STSToken.shouldRenew(cachedOidcToken)) {
            val request = HttpEntity<Any>(authorizationHeader())

            val stsTokenUri = getStsTokenUriTemplate.build().toUri()

            try {
                val response = template.exchange<STSToken>(
                        stsTokenUri,
                        HttpMethod.GET,
                        request,
                        object : ParameterizedTypeReference<STSToken>() {}
                )
                cachedOidcToken = response.body
                metric.tellEndepunktKall(METRIC_CALL_STS_SUCCESS)
            } catch (e: RestClientResponseException) {
                LOG.error("Request to get STS failed with status: ${e.rawStatusCode} and message: ${e.responseBodyAsString}")
                metric.tellHendelse(METRIC_CALL_STS_FAIL)
                throw e
            }
        }

        return cachedOidcToken!!.access_token
    }

    fun isTokenCached(): Boolean {
        return cachedOidcToken !== null
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(StsConsumer::class.java)

        const val METRIC_CALL_STS_SUCCESS = "call_sts_success"
        const val METRIC_CALL_STS_FAIL = "call_sts_fail"
    }

    private fun getStsTokenUrl(): String {
        return "$baseUrl/rest/v1/sts/token?grant_type=client_credentials&scope=openid"
    }

    private fun authorizationHeader(): HttpHeaders {
        val credentials = basicCredentials(username, password)
        val headers = HttpHeaders()
        headers.add(HttpHeaders.AUTHORIZATION, credentials)
        return headers
    }
}

data class STSToken(
        val access_token: String,
        val token_type: String,
        val expires_in: Int
) {
    // Expire 10 seconds before token expiration

    val expirationTime: LocalDateTime = LocalDateTime.now().plusSeconds(expires_in - 10L)

    companion object {
        fun shouldRenew(token: STSToken?): Boolean {
            if (token == null) {
                return true
            }

            return isExpired(token)
        }

        private fun isExpired(token: STSToken): Boolean {
            return token.expirationTime.isBefore(LocalDateTime.now())
        }
    }
}
