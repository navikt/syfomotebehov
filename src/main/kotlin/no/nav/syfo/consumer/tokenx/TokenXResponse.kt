package no.nav.syfo.consumer.tokenx

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.io.Serializable
import java.time.LocalDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class TokenXResponse(
    val access_token: String,
    val issued_token_type: String,
    val token_type: String,
    val expires_in: Long
) : Serializable

fun TokenXResponse.toTokenXToken(): TokenXToken {
    val expiresOn = LocalDateTime.now().plusSeconds(this.expires_in)
    return TokenXToken(
        accessToken = this.access_token,
        expires = expiresOn
    )
}
