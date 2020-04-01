package no.nav.syfo.testhelper;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import no.nav.security.oidc.context.*;
import no.nav.security.oidc.test.support.JwtTokenGenerator;
import no.nav.syfo.oidc.OIDCIssuer;

import java.text.ParseException;

import static no.nav.syfo.oidc.OIDCIssuer.EKSTERN;

public class OidcTestHelper {

    public static void loggInnBruker(OIDCRequestContextHolder oidcRequestContextHolder, String subject) {
        SignedJWT jwt = JwtTokenGenerator.createSignedJWT(subject);
        settOIDCValidationContext(oidcRequestContextHolder, jwt, EKSTERN);
    }

    public static void loggInnVeilederAzure(OIDCRequestContextHolder oidcRequestContextHolder, String veilederIdent) throws ParseException {
        JWTClaimsSet claimsSet = JWTClaimsSet.parse("{\"NAVident\":\"" + veilederIdent + "\"}");
        SignedJWT jwt = JwtTokenGenerator.createSignedJWT(claimsSet);
        settOIDCValidationContext(oidcRequestContextHolder, jwt, OIDCIssuer.AZURE);
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
