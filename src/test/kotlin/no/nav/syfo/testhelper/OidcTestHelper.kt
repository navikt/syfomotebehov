package no.nav.syfo.testhelper

import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.test.JwtTokenGenerator
import no.nav.syfo.api.auth.OIDCIssuer
import no.nav.syfo.api.auth.OIDCIssuer.EKSTERN
import no.nav.syfo.api.auth.tokenX.TokenXUtil
import java.text.ParseException
import java.util.*
import kotlin.collections.HashMap

object OidcTestHelper {
    const val ISS = "iss-localhost"
    const val AUD = "aud-localhost"
    const val ACR = "Level4"
    const val EXPIRY = 60L * 60L * 3600L

    @JvmStatic
    fun loggInnBruker(contextHolder: TokenValidationContextHolder, fnr: String?) {
        val jwt = generateSignedJwtToken(fnr)
        settOIDCValidationContext(contextHolder, jwt, EKSTERN)
    }

    fun loggInnBrukerTokenX(contextHolder: TokenValidationContextHolder, brukerFnr: String, clientId: String, idp: String) {
        val claimsSet = JWTClaimsSet.Builder()
            .claim("pid", brukerFnr)
            .claim("client_id", clientId)
            .claim("idp", idp)
            .build()

        val jwt = JwtTokenGenerator.createSignedJWT(claimsSet)
        settOIDCValidationContext(contextHolder, jwt, TokenXUtil.TokenXIssuer.TOKENX)
    }

    @JvmStatic
    @Throws(ParseException::class)
    fun loggInnVeilederADV2(tokenValidationContextHolder: TokenValidationContextHolder, veilederIdent: String) {
        val claimsSet = JWTClaimsSet.parse("{\"NAVident\":\"$veilederIdent\"}")
        val jwt = JwtTokenGenerator.createSignedJWT(claimsSet)
        settOIDCValidationContext(tokenValidationContextHolder, jwt, OIDCIssuer.INTERN_AZUREAD_V2)
    }

    private fun settOIDCValidationContext(contextHolder: TokenValidationContextHolder, jwt: SignedJWT, issuer: String) {
        val jwtToken = JwtToken(jwt.serialize())
        val issuerTokenMap: MutableMap<String, JwtToken> = HashMap()
        issuerTokenMap[issuer] = jwtToken
        val tokenValidationContext = TokenValidationContext(issuerTokenMap)
        contextHolder.tokenValidationContext = tokenValidationContext
    }

    @JvmStatic
    fun loggUtAlle(contextHolder: TokenValidationContextHolder) {
        contextHolder.tokenValidationContext = null
    }

    @JvmStatic
    fun generateSignedJwtToken(fnr: String?): SignedJWT {
        val now = Date()
        val claimsSet = JWTClaimsSet.Builder()
            .issuer(ISS)
            .audience(AUD)
            .jwtID(UUID.randomUUID().toString())
            .claim("pid", fnr)
            .claim("acr", ACR)
            .claim("ver", "1.0")
            .claim("nonce", "myNonce")
            .claim("auth_time", now)
            .notBeforeTime(now)
            .issueTime(now)
            .expirationTime(Date(now.getTime() + EXPIRY)).build()
        return JwtTokenGenerator.createSignedJWT(claimsSet)
    }
}
