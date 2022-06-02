package no.nav.syfo.consumer.tokenx

import java.io.Serializable
import java.time.LocalDateTime

data class TokenXToken(
    val accessToken: String,
    val expires: LocalDateTime
) : Serializable

fun TokenXToken.isExpired() = this.expires < LocalDateTime.now().plusSeconds(60)
