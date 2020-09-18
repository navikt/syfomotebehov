package no.nav.syfo.testhelper

import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.test.JwtTokenGenerator
import no.nav.syfo.api.auth.OIDCIssuer
import no.nav.syfo.api.auth.OIDCIssuer.EKSTERN
import java.text.ParseException

object OidcTestHelper {
    @JvmStatic
    fun loggInnBruker(contextHolder: TokenValidationContextHolder, subject: String?) {
        val jwt = JwtTokenGenerator.createSignedJWT(subject)
        settOIDCValidationContext(contextHolder, jwt, EKSTERN)
    }

    @JvmStatic
    @Throws(ParseException::class)
    fun loggInnVeilederAzure(contextHolder: TokenValidationContextHolder, veilederIdent: String) {
        val claimsSet = JWTClaimsSet.parse("{\"NAVident\":\"$veilederIdent\"}")
        val jwt = JwtTokenGenerator.createSignedJWT(claimsSet)
        settOIDCValidationContext(contextHolder, jwt, OIDCIssuer.AZURE)
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
