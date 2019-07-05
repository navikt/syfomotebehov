package no.nav.syfo.util;

import no.nav.security.oidc.OIDCConstants;
import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.security.oidc.context.OIDCValidationContext;
import no.nav.syfo.OIDCIssuer;
import no.nav.syfo.domain.rest.Fnr;

import static no.nav.security.oidc.OIDCConstants.OIDC_VALIDATION_CONTEXT;

public class OIDCUtil {

    public static String tokenFraOIDC(OIDCRequestContextHolder contextHolder, String issuer) {
        OIDCValidationContext context = (OIDCValidationContext) contextHolder
                .getRequestAttribute(OIDCConstants.OIDC_VALIDATION_CONTEXT);
        return context.getToken(issuer).getIdToken();
    }

    public static Fnr fnrFraOIDCEkstern(OIDCRequestContextHolder contextHolder) {
        OIDCValidationContext context = (OIDCValidationContext) contextHolder
                .getRequestAttribute(OIDCConstants.OIDC_VALIDATION_CONTEXT);
        return Fnr.of(context.getClaims(OIDCIssuer.EKSTERN).getClaimSet().getSubject());
    }

    public static String getSubjectFromOIDCToken(OIDCRequestContextHolder contextHolder, String issuerName) {
        OIDCValidationContext context = (OIDCValidationContext) contextHolder
                .getRequestAttribute(OIDC_VALIDATION_CONTEXT);
        return context.getClaims(issuerName).getClaimSet().getSubject();
    }
}
