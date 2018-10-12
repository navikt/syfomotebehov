package no.nav.syfo.controller;

import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.syfo.LocalApplication;
import no.nav.syfo.domain.rest.Fnr;
import no.nav.syfo.domain.rest.Motebehov;
import no.nav.syfo.domain.rest.MotebehovSvar;
import no.nav.syfo.domain.rest.NyttMotebehov;
import no.nav.syfo.mock.AktoerMock;
import no.nav.syfo.repository.dao.MotebehovDAO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import java.util.List;

import static no.nav.syfo.service.VeilederTilgangService.FNR;
import static no.nav.syfo.service.VeilederTilgangService.TILGANG_TIL_BRUKER_PATH;
import static no.nav.syfo.util.OidcTestHelper.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.client.ExpectedCount.manyTimes;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.web.util.UriComponentsBuilder.fromHttpUrl;

/**
 * Komponent / blackbox test av møtebehovsfunskjonaliteten - test at input til endepunktet (controlleren, for enkelhets skyld)
 * lagres og hentes riktig fra minnedatabasen.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = LocalApplication.class)
@DirtiesContext
public class MotebehovVeilederComponentTest {

    private static final String ARBEIDSTAKER_FNR = "12345678910";
    private static final String LEDER_FNR = "10987654321";
    private static final String LEDER_AKTORID = AktoerMock.mockAktorId(LEDER_FNR);
    private static final String VIRKSOMHETSNUMMER = "1234";
    private static final String VEILEDER_ID = "Z999999";
    @Value("${tilgangskontrollapi.url}")
    private String tilgangskontrollUrl;

    @Inject
    private MotebehovBrukerController motebehovController;

    @Inject
    private MotebehovVeilederController motebehovVeilederController;

    @Inject
    private OIDCRequestContextHolder oidcRequestContextHolder;

    @Inject
    private MotebehovDAO motebehovDAO;

    @Inject
    private RestTemplate restTemplate;

    private MockRestServiceServer mockRestServiceServer;

    @Before
    public void setUp() {
        cleanDB();
        this.mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @After
    public void tearDown() {
        // Verify all expectations met
        mockRestServiceServer.verify();
        loggUtAlle(oidcRequestContextHolder);
        cleanDB();
    }

    @Test
    public void arbeidsgiverLagrerOgVeilederHenterMotebehov() {
        // Nærmeste leder lagrer møtebehov
        loggInnBruker(oidcRequestContextHolder, LEDER_FNR);
        final MotebehovSvar motebehovSvar = new MotebehovSvar()
                .harMotebehov(true)
                .friskmeldingForventning("Om en uke")
                .tiltak("Krykker")
                .tiltakResultat("Kommer seg fremover")
                .forklaring("");

        final NyttMotebehov lagreMotebehov = new NyttMotebehov()
                .arbeidstakerFnr(Fnr.of(ARBEIDSTAKER_FNR))
                .virksomhetsnummer(VIRKSOMHETSNUMMER)
                .motebehovSvar(
                        motebehovSvar
                );

        motebehovController.lagreMotebehov(lagreMotebehov);

        // Veileder henter møtebehov
        loggInnVeileder(oidcRequestContextHolder, VEILEDER_ID);
        mockSvarFraSyfoTilgangskontroll(ARBEIDSTAKER_FNR, OK);

        List<Motebehov> motebehovListe = motebehovVeilederController.hentMotebehovListe(ARBEIDSTAKER_FNR);
        assertThat(motebehovListe).size().isOne();

        Motebehov motebehov = motebehovListe.get(0);
        assertThat(motebehov.opprettetAv).isEqualTo(LEDER_AKTORID);
        assertThat(motebehov.arbeidstakerFnr).isEqualTo(ARBEIDSTAKER_FNR);
        assertThat(motebehov.virksomhetsnummer).isEqualTo(VIRKSOMHETSNUMMER);
        assertThat(motebehov.motebehovSvar).isEqualToComparingFieldByField(motebehovSvar);
    }


    private void mockSvarFraSyfoTilgangskontroll(String fnr, HttpStatus status){
        String uriString = fromHttpUrl(tilgangskontrollUrl)
                .path(TILGANG_TIL_BRUKER_PATH)
                .queryParam(FNR, fnr)
                .toUriString();

        String idToken = oidcRequestContextHolder.getOIDCValidationContext().getToken("intern").getIdToken();

        mockRestServiceServer.expect(manyTimes(), requestTo(uriString))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(AUTHORIZATION, "Bearer " + idToken))
                .andRespond(withStatus(status));
    }

    private void cleanDB() {
        motebehovDAO.nullstillMotebehov(LEDER_AKTORID);
    }

}