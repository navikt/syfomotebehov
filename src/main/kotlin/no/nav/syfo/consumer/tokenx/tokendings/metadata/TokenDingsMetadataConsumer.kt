package no.nav.syfo.consumer.tokenx.tokendings.metadata

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate

@Component
class TokenDingsMetadataConsumer(
    private val restTemplate: RestTemplate,
    @Value("\${token.x.well.known.url}") private val wellKnownUrl: String
) {
    fun getTokenDingsMetadata(): TokenDingsMetadata {
        try {
            val response = restTemplate.exchange(
                wellKnownUrl,
                HttpMethod.GET,
                entity(),
                TokenDingsMetadata::class.java
            )

            return response.body!!
        } catch (e: RestClientResponseException) {
            log.error(
                "Call requesting OauthServerConfigurationMetadata failed with status: ${e.rawStatusCode} and message: ${e.responseBodyAsString}",
                e
            )
            throw e
        }
    }

    private fun entity(): HttpEntity<String> {
        val headers = HttpHeaders()
        headers.accept = listOf(MediaType.APPLICATION_JSON)
        return HttpEntity(headers)
    }

    companion object {
        private val log = LoggerFactory.getLogger(TokenDingsMetadataConsumer::class.java)
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class TokenDingsMetadata(
    @JsonProperty(value = "issuer", required = true) val issuer: String,
    @JsonProperty(value = "token_endpoint", required = true) val tokenEndpoint: String,
    @JsonProperty(value = "jwks_uri", required = true) val jwksUri: String,
    @JsonProperty(value = "authorization_endpoint", required = false) var authorizationEndpoint: String = ""
)
