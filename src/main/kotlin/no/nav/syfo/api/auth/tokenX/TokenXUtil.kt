package no.nav.syfo.api.auth.tokenX

import javax.ws.rs.ForbiddenException
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer

object TokenXUtil {
    @Throws(ForbiddenException::class)
    fun validateTokenXClaims(
        contextHolder: TokenValidationContextHolder,
        requestedTokenxIdp: String,
        vararg requestedClientId: String,
    ): JwtTokenClaims {
        val context = contextHolder.tokenValidationContext
        val claims = context.getClaims(TokenXIssuer.TOKENX)
        val clientId = claims.getStringClaim("client_id")

        if (!requestedClientId.toList().contains(clientId)) {
            throw ForbiddenException("Uventet client id $clientId")
        }
        val idp = claims.getStringClaim("idp")
        if (idp != requestedTokenxIdp) {
            // Check that  Idporten was IDP for tokenX
            throw ForbiddenException("Uventet idp $idp, requestedTokenxIdp: $requestedTokenxIdp")
        }
        return claims
    }

    fun JwtTokenClaims.fnrFromIdportenTokenX(): Fodselsnummer {
        return Fodselsnummer(this.getStringClaim("pid"))
    }

    fun fnrFromIdportenTokenX(contextHolder: TokenValidationContextHolder): Fodselsnummer {
        val context = contextHolder.tokenValidationContext
        val claims = context.getClaims(TokenXIssuer.TOKENX)
        return Fodselsnummer(claims.getStringClaim("pid"))
    }

    fun tokenFromTokenX(contextHolder: TokenValidationContextHolder): String {
        val context = contextHolder.tokenValidationContext
        return context.getJwtToken(TokenXIssuer.TOKENX).tokenAsString
    }

    object TokenXIssuer {
        const val TOKENX = "tokenx"
    }
}
