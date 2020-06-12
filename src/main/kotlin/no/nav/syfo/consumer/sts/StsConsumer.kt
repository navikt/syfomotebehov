package no.nav.syfo.consumer.sts

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.syfo.metric.Metric
import no.nav.syfo.util.basicCredentials
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.*
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@Service
class StsConsumer(
    private val metric: Metric,
    @Value("\${security.token.service.rest.url}") private val baseUrl: String,
    @Value("\${srv.username}") private val username: String,
    @Value("\${srv.password}") private val password: String
) {
    private var cachedOidcToken: STSToken? = null

    private val webClient = WebClient
        .builder()
        .baseUrl(baseUrl)
        .defaultHeader(HttpHeaders.AUTHORIZATION, basicCredentials(username, password))
        .build()

    fun token(): String {
        if (STSToken.shouldRenew(cachedOidcToken)) {
            val response = webClient
                .get()
                .uri(getStsTokenUrl())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus({ obj: HttpStatus -> obj.is4xxClientError }) { response ->
                    logError(response)
                    Mono.error(RuntimeException("4xx"))
                }
                .onStatus({ obj: HttpStatus -> obj.is5xxServerError }) { response ->
                    logError(response)
                    Mono.error(RuntimeException("5xx"))
                }
                .bodyToMono<STSToken>()
                .block()
            cachedOidcToken = response
            metric.tellEndepunktKall(METRIC_CALL_STS_SUCCESS)
        }
        return cachedOidcToken!!.access_token
    }

    fun logError(response: ClientResponse) {
        LOG.error("Request to get STS failed with status: ${response.rawStatusCode()}")
        metric.tellHendelse(METRIC_CALL_STS_FAIL)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(StsConsumer::class.java)

        const val METRIC_CALL_STS_SUCCESS = "call_sts_success"
        const val METRIC_CALL_STS_FAIL = "call_sts_fail"
    }

    private fun getStsTokenUrl(): String {
        return "$baseUrl/rest/v1/sts/token?grant_type=client_credentials&scope=openid"
    }
}

data class STSToken(
    @JsonProperty(value = "access_token", required = true)
    val access_token: String,
    @JsonProperty(value = "token_type", required = true)
    val token_type: String,
    @JsonProperty(value = "expires_in", required = true)
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
