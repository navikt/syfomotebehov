package no.nav.syfo.testhelper;

import com.nimbusds.jwt.SignedJWT;
import no.nav.security.oidc.context.*;
import no.nav.security.spring.oidc.test.JwtTokenGenerator;

import static no.nav.syfo.OIDCIssuer.EKSTERN;
import static no.nav.syfo.OIDCIssuer.INTERN;

public class OidcTestHelper {

    public static void loggInnBruker(OIDCRequestContextHolder oidcRequestContextHolder, String subject) {
        SignedJWT jwt = JwtTokenGenerator.createSignedJWT(subject);
        settOIDCValidationContext(oidcRequestContextHolder, jwt, EKSTERN);
    }

    public static void loggInnVeileder(OIDCRequestContextHolder oidcRequestContextHolder, String subject) {
        SignedJWT jwt = JwtTokenGenerator.createSignedJWT(subject);
        settOIDCValidationContext(oidcRequestContextHolder, jwt, INTERN);
    }

    private static void settOIDCValidationContext(OIDCRequestContextHolder oidcRequestContextHolder, SignedJWT jwt, String issuer) {
        TokenContext tokenContext = new TokenContext(issuer, jwt.serialize());
        OIDCClaims oidcClaims = new OIDCClaims(jwt);
        OIDCValidationContext oidcValidationContext = new OIDCValidationContext();
        oidcValidationContext.addValidatedToken(issuer, tokenContext, oidcClaims);
        oidcRequestContextHolder.setOIDCValidationContext(oidcValidationContext);
    }

    public static void loggUtAlle(OIDCRequestContextHolder oidcRequestContextHolder) {
        oidcRequestContextHolder.setOIDCValidationContext(null);
    }

}
