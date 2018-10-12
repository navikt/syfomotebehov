package no.nav.syfo.util;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.oidc.OIDCConstants;
import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.security.oidc.context.OIDCValidationContext;
import no.nav.syfo.domain.rest.Fnr;

public class OIDCUtil {

    public static String tokenFraOIDC(OIDCValidationContext contextHolder) {
        return contextHolder.getToken("selvbetjening").getIdToken();

    }

    public static Fnr fnrFraOIDC(OIDCRequestContextHolder contextHolder) {
        OIDCValidationContext context = (OIDCValidationContext) contextHolder
                .getRequestAttribute(OIDCConstants.OIDC_VALIDATION_CONTEXT);
        return Fnr.of(context.getClaims("selvbetjening").getClaimSet().getSubject());
    }

}
