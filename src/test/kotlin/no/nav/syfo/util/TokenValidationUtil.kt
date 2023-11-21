package no.nav.syfo.util

import com.nimbusds.jwt.SignedJWT
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.syfo.api.auth.OIDCIssuer
import no.nav.syfo.api.auth.tokenX.TokenXUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class TokenValidationUtil() {
    @Autowired
    private lateinit var contextHolder: TokenValidationContextHolder

    @Autowired
    private lateinit var mockOAuthServer: MockOAuth2Server

    fun logInAsDialogmoteUser(
        userFnr: String
    ) {
        val signedJwtToken = mockOAuthServer.issueToken(
            issuerId = "tokenx-client-id",
            subject = "dialogmote-frontend",
            audience = "syfomotebehovsrv",
            claims = mapOf(
                "client_id" to "dialogmote-frontend",
                "acr" to "Level4",
                "pid" to userFnr
            ),
            expiry = 60L
        )
        setTokenInValidationContext(signedJwtToken, TokenXUtil.TokenXIssuer.TOKENX)
    }

    fun logInAsNavCounselor(
        username: String
    ) {
        val signedJwtToken = mockOAuthServer.issueToken(
            issuerId = "azuread-v2-issuer",
            subject = "modiasyfoperson",
            audience = "syfomotebehovsrv",
            claims = mapOf(
                "NAVident" to username
            ),
            expiry = 60L
        )
        setTokenInValidationContext(signedJwtToken, OIDCIssuer.INTERN_AZUREAD_V2)
    }
    private fun setTokenInValidationContext(signedJwtToken: SignedJWT, issuer: String) {
        contextHolder.tokenValidationContext = TokenValidationContext(
            mutableMapOf(issuer to JwtToken(signedJwtToken.serialize()))
        )
    }
}
