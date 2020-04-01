package no.nav.syfo.service;

import com.nimbusds.jwt.SignedJWT;
import no.nav.security.oidc.context.*;
import no.nav.security.oidc.test.support.JwtTokenGenerator;
import no.nav.syfo.aktorregister.domain.Fodselsnummer;
import no.nav.syfo.brukertilgang.BrukertilgangConsumer;
import no.nav.syfo.brukertilgang.BrukertilgangService;
import no.nav.syfo.pdl.PdlConsumer;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.syfo.oidc.OIDCIssuer.EKSTERN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BrukertilgangskontrollServiceTest {

    @Mock
    private OIDCRequestContextHolder oidcRequestContextHolder;
    @Mock
    private BrukertilgangConsumer brukertilgangConsumer;
    @Mock
    private PdlConsumer pdlConsumer;
    @InjectMocks
    private BrukertilgangService tilgangskontrollService;

    private static final String INNLOGGET_FNR = "15065933818";
    private static final String SPOR_OM_FNR = "12345678902";

    @Before
    public void setup() {
        mockOIDC(INNLOGGET_FNR);

        when(pdlConsumer.isKode6(new Fodselsnummer(SPOR_OM_FNR))).thenReturn(false);
    }

    @After
    public void tearDown() {
        oidcRequestContextHolder.setOIDCValidationContext(null);
    }

    @Test
    public void harTilgangTilOppslaattBrukerGirFalseNaarOppslaattBrukerErKode6() {
        when(brukertilgangConsumer.hasAccessToAnsatt(SPOR_OM_FNR)).thenReturn(true);
        when(pdlConsumer.isKode6(new Fodselsnummer(SPOR_OM_FNR))).thenReturn(true);

        boolean tilgang = tilgangskontrollService.harTilgangTilOppslaattBruker(INNLOGGET_FNR, SPOR_OM_FNR);
        assertThat(tilgang).isFalse();
    }

    @Test
    public void harTilgangTilOppslaattBrukerGirTrueNaarManSporOmSegSelv() {
        boolean tilgang = tilgangskontrollService.harTilgangTilOppslaattBruker(INNLOGGET_FNR, INNLOGGET_FNR);
        assertThat(tilgang).isTrue();
    }

    @Test
    public void harTilgangTilOppslaattBrukerGirTrueNaarManSporOmEnAnsatt() {
        when(brukertilgangConsumer.hasAccessToAnsatt(SPOR_OM_FNR)).thenReturn(true);
        boolean tilgang = tilgangskontrollService.harTilgangTilOppslaattBruker(INNLOGGET_FNR, SPOR_OM_FNR);
        assertThat(tilgang).isTrue();
    }

    @Test
    public void harTilgangTilOppslaattBrukerGirFalseNaarManSporOmEnSomIkkeErSegSelvOgIkkeAnsatt() {
        when(brukertilgangConsumer.hasAccessToAnsatt(SPOR_OM_FNR)).thenReturn(false);
        boolean tilgang = tilgangskontrollService.harTilgangTilOppslaattBruker(INNLOGGET_FNR, SPOR_OM_FNR);
        assertThat(tilgang).isFalse();
    }

    @Test
    public void sporOmNoenAndreEnnSegSelvGirFalseNaarManSporOmSegSelv() {
        boolean tilgang = tilgangskontrollService.sporOmNoenAndreEnnSegSelvEllerEgneAnsatte(INNLOGGET_FNR, INNLOGGET_FNR);
        assertThat(tilgang).isFalse();
    }

    @Test
    public void sporOmNoenAndreEnnSegSelvGirFalseNaarManSporOmEnAnsatt() {
        when(brukertilgangConsumer.hasAccessToAnsatt(SPOR_OM_FNR)).thenReturn(true);
        boolean tilgang = tilgangskontrollService.sporOmNoenAndreEnnSegSelvEllerEgneAnsatte(INNLOGGET_FNR, SPOR_OM_FNR);
        assertThat(tilgang).isFalse();
    }

    @Test
    public void sporOmNoenAndreEnnSegSelvGirTrueNaarManSporOmEnSomIkkeErSegSelvOgIkkeAnsatt() {
        when(brukertilgangConsumer.hasAccessToAnsatt(SPOR_OM_FNR)).thenReturn(false);
        boolean tilgang = tilgangskontrollService.sporOmNoenAndreEnnSegSelvEllerEgneAnsatte(INNLOGGET_FNR, SPOR_OM_FNR);
        assertThat(tilgang).isTrue();
    }

    private void mockOIDC(String subject) {
        SignedJWT jwt = JwtTokenGenerator.createSignedJWT(subject);
        String issuer = EKSTERN;
        TokenContext tokenContext = new TokenContext(issuer, jwt.serialize());
        OIDCClaims oidcClaims = new OIDCClaims(jwt);
        OIDCValidationContext oidcValidationContext = new OIDCValidationContext();
        oidcValidationContext.addValidatedToken(issuer, tokenContext, oidcClaims);
        oidcRequestContextHolder.setOIDCValidationContext(oidcValidationContext);
    }
}
