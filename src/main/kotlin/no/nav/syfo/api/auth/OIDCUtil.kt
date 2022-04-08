package no.nav.syfo.api.auth

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer

object OIDCUtil {
    const val PID_CLAIM_NAME = "pid"

    fun tokenFraOIDC(contextHolder: TokenValidationContextHolder, issuer: String?): String {
        val context = contextHolder.tokenValidationContext
        return context.getJwtToken(issuer).tokenAsString
    }

    fun fnrFraOIDCEkstern(contextHolder: TokenValidationContextHolder): Fodselsnummer {
        val context = contextHolder.tokenValidationContext
        val jwtTokenClaims = context.getClaims(OIDCIssuer.EKSTERN)
        val personalIdentifier = jwtTokenClaims.getStringClaim(PID_CLAIM_NAME)
        return Fodselsnummer(personalIdentifier)
    }
}

fun getSubjectInternADV2(contextHolder: TokenValidationContextHolder): String {
    val context = contextHolder.tokenValidationContext
    return context.getClaims(OIDCIssuer.INTERN_AZUREAD_V2).getStringClaim(OIDCClaim.NAVIDENT)
}
