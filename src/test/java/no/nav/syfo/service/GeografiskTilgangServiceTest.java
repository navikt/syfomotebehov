package no.nav.syfo.service;

import com.nimbusds.jwt.SignedJWT;
import no.nav.security.oidc.context.OIDCClaims;
import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.security.oidc.context.OIDCValidationContext;
import no.nav.security.oidc.context.TokenContext;
import no.nav.security.spring.oidc.test.JwtTokenGenerator;
import no.nav.syfo.consumer.ws.AktoerConsumer;
import no.nav.syfo.consumer.ws.OrganisasjonEnhetConsumer;
import no.nav.syfo.consumer.ws.PersonConsumer;
import no.nav.syfo.consumer.ws.SykefravaeroppfoelgingConsumer;
import no.nav.syfo.util.Toggle;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.ws.rs.ForbiddenException;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class GeografiskTilgangServiceTest {

    @Mock
    private OIDCRequestContextHolder oidcRequestContextHolder;
    @Mock
    private OrganisasjonEnhetConsumer organisasjonEnhetConsumer;
    @Mock
    private PersonConsumer personConsumer;
    @InjectMocks
    private GeografiskTilgangService geografiskTilgangService;

    private static final String INNLOGGET_FNR = "15065933818";
    private static final String BRUKER_FNR = "1234567890";
    private static final String GEOGRAFISK_TILKNYTNING = "Oslo";

    @Before
    public void setup() {
        mockOIDC(INNLOGGET_FNR);

        when(personConsumer.hentGeografiskTilknytning(BRUKER_FNR)).thenReturn(GEOGRAFISK_TILKNYTNING);
        when(organisasjonEnhetConsumer.finnNAVKontorForGT(GEOGRAFISK_TILKNYTNING)).thenReturn(asList("0330"));
    }

    @After
    public void tearDown() {
        oidcRequestContextHolder.setOIDCValidationContext(null);
    }

    @Test
    public void returnerFalseHvisBrukerIkkeIPilotkontor(){
        Toggle.pilotKontorer = "ikkeSykmeldtesKontor";

        boolean erBrukerTilhorendeMotebehovPilot = geografiskTilgangService.erBrukerTilhorendeMotebehovPilot(BRUKER_FNR);
        assertThat(erBrukerTilhorendeMotebehovPilot).isFalse();
    }

    @Test
    public void returnerTrueHvisBrukerErIPilotkontor(){
        Toggle.pilotKontorer = "0330";

        boolean erBrukerTilhorendeMotebehovPilot = geografiskTilgangService.erBrukerTilhorendeMotebehovPilot(BRUKER_FNR);
        assertThat(erBrukerTilhorendeMotebehovPilot).isTrue();
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
