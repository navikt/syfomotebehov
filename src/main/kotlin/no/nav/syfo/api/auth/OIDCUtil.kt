package no.nav.syfo.api.auth

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer

object OIDCUtil {
    fun tokenFraOIDC(contextHolder: TokenValidationContextHolder, issuer: String?): String {
        val context = contextHolder.tokenValidationContext
        return context.getJwtToken(issuer).tokenAsString
    }

    fun fnrFraOIDCEkstern(contextHolder: TokenValidationContextHolder): Fodselsnummer {
        val context = contextHolder.tokenValidationContext
        return Fodselsnummer(context.getClaims(OIDCIssuer.EKSTERN).subject)
    }
}

fun getSubjectInternAD(contextHolder: TokenValidationContextHolder): String {
    val context = contextHolder.tokenValidationContext
    return context.getClaims(OIDCIssuer.AZURE).getStringClaim(OIDCClaim.NAVIDENT)
}
