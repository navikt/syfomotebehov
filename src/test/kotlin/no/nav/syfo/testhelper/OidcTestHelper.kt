package no.nav.syfo.testhelper

import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.test.JwtTokenGenerator
import no.nav.syfo.api.auth.OIDCIssuer
import no.nav.syfo.api.auth.tokenX.TokenXUtil
import java.text.ParseException
import java.util.*

object OidcTestHelper {

    fun loggInnBrukerTokenX(contextHolder: TokenValidationContextHolder, brukerFnr: String, clientId: String) {
        val claimsSet = JWTClaimsSet.Builder()
            .claim("pid", brukerFnr)
            .claim("client_id", clientId)
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
}
