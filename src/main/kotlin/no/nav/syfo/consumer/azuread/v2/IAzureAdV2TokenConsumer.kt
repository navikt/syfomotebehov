package no.nav.syfo.consumer.azuread.v2

import org.springframework.http.HttpEntity
import org.springframework.util.MultiValueMap

interface IAzureAdV2TokenConsumer {
    fun getOnBehalfOfToken(
        scopeClientId: String,
        token: String,
    ): String

    fun getSystemToken(scopeClientId: String): String

    fun systemTokenRequestEntity(scopeClientId: String): HttpEntity<MultiValueMap<String, String>>
}
