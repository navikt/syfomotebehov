package no.nav.syfo.service;

import com.nimbusds.jwt.SignedJWT;
import no.nav.security.oidc.context.*;
import no.nav.security.spring.oidc.test.JwtTokenGenerator;
import no.nav.syfo.oidc.OIDCIssuer;
import no.nav.syfo.consumer.ws.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class BrukertilgangskontrollServiceTest {

    @Mock
    private OIDCRequestContextHolder oidcRequestContextHolder;
    @Mock
    private AktoerConsumer aktorConsumer;
    @Mock
    private PersonConsumer personConsumer;
    @Mock
    private SykefravaeroppfoelgingConsumer sykefravaeroppfoelgingConsumer;
    @InjectMocks
    private BrukertilgangService tilgangskontrollService;

    private static final String INNLOGGET_FNR = "15065933818";
    private static final String INNLOGGET_AKTOERID = "1234567890123";
    private static final String SPOR_OM_FNR = "12345678902";
    private static final String SPOR_OM_AKTOERID = "1234567890122";

    @Before
    public void setup() {
        mockOIDC(INNLOGGET_FNR);

        when(aktorConsumer.hentAktoerIdForFnr(INNLOGGET_FNR)).thenReturn(INNLOGGET_AKTOERID);
        when(aktorConsumer.hentAktoerIdForFnr(SPOR_OM_FNR)).thenReturn(SPOR_OM_AKTOERID);
        when(personConsumer.erBrukerKode6(SPOR_OM_AKTOERID)).thenReturn(false);
    }

    @After
    public void tearDown() {
        oidcRequestContextHolder.setOIDCValidationContext(null);
    }

    @Test
    public void harTilgangTilOppslaattBrukerGirFalseNaarOppslaattBrukerErKode6() {
        when(sykefravaeroppfoelgingConsumer.hentAnsatteAktorId(INNLOGGET_AKTOERID, OIDCIssuer.EKSTERN)).thenReturn(asList(
                SPOR_OM_AKTOERID
        ));
        when(personConsumer.erBrukerKode6(SPOR_OM_AKTOERID)).thenReturn(true);

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
        when(sykefravaeroppfoelgingConsumer.hentAnsatteAktorId(INNLOGGET_AKTOERID, OIDCIssuer.EKSTERN)).thenReturn(asList(
                SPOR_OM_AKTOERID
        ));
        boolean tilgang = tilgangskontrollService.harTilgangTilOppslaattBruker(INNLOGGET_FNR, SPOR_OM_FNR);
        assertThat(tilgang).isTrue();
    }

    @Test
    public void harTilgangTilOppslaattBrukerGirFalseNaarManSporOmEnSomIkkeErSegSelvOgIkkeAnsatt() {
        when(sykefravaeroppfoelgingConsumer.hentAnsatteAktorId(INNLOGGET_AKTOERID, OIDCIssuer.EKSTERN)).thenReturn(asList(

        ));
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
        when(sykefravaeroppfoelgingConsumer.hentAnsatteAktorId(INNLOGGET_AKTOERID, OIDCIssuer.EKSTERN)).thenReturn(asList(
                SPOR_OM_AKTOERID
        ));
        boolean tilgang = tilgangskontrollService.sporOmNoenAndreEnnSegSelvEllerEgneAnsatte(INNLOGGET_FNR, SPOR_OM_FNR);
        assertThat(tilgang).isFalse();
    }

    @Test
    public void sporOmNoenAndreEnnSegSelvGirTrueNaarManSporOmEnSomIkkeErSegSelvOgIkkeAnsatt() {
        when(sykefravaeroppfoelgingConsumer.hentAnsatteAktorId(INNLOGGET_AKTOERID, OIDCIssuer.EKSTERN)).thenReturn(asList(

        ));
        boolean tilgang = tilgangskontrollService.sporOmNoenAndreEnnSegSelvEllerEgneAnsatte(INNLOGGET_FNR, SPOR_OM_FNR);
        assertThat(tilgang).isTrue();
    }


    @Test
    public void sporOmNoenAndreEnnSegSelvEllerEgneAnsatteEllerLedereGirFalseNaaerManSporOmSegSelv() {
        boolean tilgang = tilgangskontrollService.sporOmNoenAndreEnnSegSelvEllerEgneAnsatteEllerLedere(INNLOGGET_FNR, INNLOGGET_FNR);
        assertThat(tilgang).isFalse();
    }

    @Test
    public void sporOmNoenAndreEnnSegSelvEllerEgneAnsatteEllerLedereGirFalseNaarManSporOmEnAnsatt() {
        when(sykefravaeroppfoelgingConsumer.hentAnsatteAktorId(INNLOGGET_AKTOERID, OIDCIssuer.EKSTERN)).thenReturn(asList(
                SPOR_OM_AKTOERID
        ));
        boolean tilgang = tilgangskontrollService.sporOmNoenAndreEnnSegSelvEllerEgneAnsatteEllerLedere(INNLOGGET_FNR, SPOR_OM_FNR);
        assertThat(tilgang).isFalse();
    }

    @Test
    public void sporOmNoenAndreEnnSegSelvEllerEgneAnsatteEllerLedereGirFalseNaarManSporOmEnLeder() {
        when(sykefravaeroppfoelgingConsumer.hentAnsatteAktorId(INNLOGGET_AKTOERID, OIDCIssuer.EKSTERN)).thenReturn(asList(

        ));
        when(sykefravaeroppfoelgingConsumer.hentNaermesteLederAktoerIdListe(INNLOGGET_AKTOERID, OIDCIssuer.EKSTERN)).thenReturn(asList(
                SPOR_OM_AKTOERID
        ));
        boolean tilgang = tilgangskontrollService.sporOmNoenAndreEnnSegSelvEllerEgneAnsatteEllerLedere(INNLOGGET_FNR, SPOR_OM_FNR);
        assertThat(tilgang).isFalse();
    }

    @Test
    public void sporOmNoenAndreEnnSegSelvEllerEgneAnsatteEllerLedereGirTrueNaarManSporOmEnSomIkkeErSegSelvOgIkkeAnsatt() {
        when(sykefravaeroppfoelgingConsumer.hentAnsatteAktorId(INNLOGGET_AKTOERID, OIDCIssuer.EKSTERN)).thenReturn(asList(

        ));
        when(sykefravaeroppfoelgingConsumer.hentNaermesteLederAktoerIdListe(INNLOGGET_AKTOERID, OIDCIssuer.EKSTERN)).thenReturn(asList(

        ));
        boolean tilgang = tilgangskontrollService.sporOmNoenAndreEnnSegSelvEllerEgneAnsatteEllerLedere(INNLOGGET_FNR, SPOR_OM_FNR);
        assertThat(tilgang).isTrue();
    }

    private void mockOIDC(String subject) {
        //OIDC-hack - legg til token og oidcclaims for en test-person
        SignedJWT jwt = JwtTokenGenerator.createSignedJWT(subject);
        String issuer = "selvbetjening";
        TokenContext tokenContext = new TokenContext(issuer, jwt.serialize());
        OIDCClaims oidcClaims = new OIDCClaims(jwt);
        OIDCValidationContext oidcValidationContext = new OIDCValidationContext();
        oidcValidationContext.addValidatedToken(issuer, tokenContext, oidcClaims);
        oidcRequestContextHolder.setOIDCValidationContext(oidcValidationContext);
    }
}
