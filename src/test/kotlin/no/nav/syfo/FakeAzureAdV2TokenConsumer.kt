package no.nav.syfo

import no.nav.syfo.consumer.azuread.v2.IAzureAdV2TokenConsumer
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpEntity
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap

@Profile("local", "!remote")
@Component
class FakeAzureAdV2TokenConsumer : IAzureAdV2TokenConsumer {
    val logger = LoggerFactory.getLogger(this.javaClass)
    override fun getOnBehalfOfToken(scopeClientId: String, token: String): String {
        return token
    }

    override fun getSystemToken(scopeClientId: String): String {
        return "token"
    }

    override fun systemTokenRequestEntity(scopeClientId: String): HttpEntity<MultiValueMap<String, String>> {
        return ResponseEntity.ok(LinkedMultiValueMap())
    }

    init {
        logger.warn("!! ----- Running with fake AzureAdV2TokenConsumer  ----- !!")
    }
}
