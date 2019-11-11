package no.nav.syfo.util;

import no.nav.security.oidc.OIDCConstants;
import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.security.oidc.context.OIDCValidationContext;
import no.nav.syfo.oidc.OIDCClaim;
import no.nav.syfo.oidc.OIDCIssuer;
import no.nav.syfo.domain.rest.Fnr;

import java.text.ParseException;

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

    public static String getSubjectInternAD(OIDCRequestContextHolder contextHolder) {
        OIDCValidationContext context = (OIDCValidationContext) contextHolder
                .getRequestAttribute(OIDC_VALIDATION_CONTEXT);
        try {
            return context.getClaims(OIDCIssuer.AZURE).getClaimSet().getStringClaim(OIDCClaim.NAVIDENT);
        } catch (ParseException e) {
            throw new RuntimeException("Klarte ikke hente veileder-ident ut av OIDC-token (Azure)");
        }
    }
}
