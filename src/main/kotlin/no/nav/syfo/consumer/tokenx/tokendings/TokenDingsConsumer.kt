package no.nav.syfo.consumer.tokenx.tokendings

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import no.nav.syfo.consumer.tokenx.TokenXResponse
import no.nav.syfo.consumer.tokenx.toTokenXToken
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import java.time.Instant
import java.util.Date
import java.util.UUID
import javax.inject.Inject

@Component
class TokenDingsConsumer
    @Inject
    constructor(
        private val restTemplate: RestTemplate,
        @Value("\${token.x.client.id}") private val clientId: String,
        @Value("\${token.x.private.jwk}") private val privateJwk: String,
        @Value("\${token.x.token.endpoint}") private val tokenxEndpoint: String,
    ) {
        fun exchangeToken(
            subjectToken: String,
            targetApp: String,
        ): String {
            val requestEntity = requestEntity(subjectToken, tokenxEndpoint, targetApp)

            val response =
                restTemplate.exchange(
                    tokenxEndpoint,
                    HttpMethod.POST,
                    requestEntity,
                    TokenXResponse::class.java,
                )
            return response.body!!.toTokenXToken().accessToken
        }

        private fun requestEntity(
            subjectToken: String,
            tokenEndpoint: String,
            targetApp: String,
        ): HttpEntity<MultiValueMap<String, String>> {
            val headers = HttpHeaders()
            headers.contentType = MediaType.APPLICATION_FORM_URLENCODED
            val body: MultiValueMap<String, String> = LinkedMultiValueMap()
            body.add("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange")
            body.add("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
            body.add("client_assertion", getClientAssertion(tokenEndpoint))
            body.add("subject_token_type", "urn:ietf:params:oauth:token-type:jwt")
            body.add("subject_token", subjectToken)
            body.add("audience", targetApp)
            return HttpEntity(body, headers)
        }

        fun getClientAssertion(tokenEndpoint: String): String {
            val rsaKey = RSAKey.parse(privateJwk)
            val now = Date.from(Instant.now())
            return JWTClaimsSet
                .Builder()
                .issuer(clientId)
                .subject(clientId)
                .audience(tokenEndpoint)
                .issueTime(now)
                .expirationTime(Date.from(Instant.now().plusSeconds(60)))
                .jwtID(UUID.randomUUID().toString())
                .notBeforeTime(now)
                .build()
                .sign(rsaKey)
                .serialize()
        }
    }

internal fun JWTClaimsSet.sign(rsaKey: RSAKey): SignedJWT =
    SignedJWT(
        JWSHeader
            .Builder(JWSAlgorithm.RS256)
            .keyID(rsaKey.keyID)
            .type(JOSEObjectType.JWT)
            .build(),
        this,
    ).apply {
        sign(RSASSASigner(rsaKey.toPrivateKey()))
    }
