package no.nav.syfo.api.auth

import no.nav.security.token.support.core.context.TokenValidationContextHolder
object OIDCUtil {

    fun tokenFraOIDC(contextHolder: TokenValidationContextHolder, issuer: String?): String {
        val context = contextHolder.tokenValidationContext
        return context.getJwtToken(issuer).tokenAsString
    }
}

fun getSubjectInternADV2(contextHolder: TokenValidationContextHolder): String {
    val context = contextHolder.tokenValidationContext
    return context.getClaims(OIDCIssuer.INTERN_AZUREAD_V2).getStringClaim(OIDCClaim.NAVIDENT)
}
