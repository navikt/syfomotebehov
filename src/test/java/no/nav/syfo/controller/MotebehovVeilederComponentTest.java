package no.nav.syfo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.syfo.LocalApplication;
import no.nav.syfo.domain.rest.*;
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
import java.time.LocalDateTime;
import java.util.List;

import static java.util.Collections.singletonList;
import static no.nav.syfo.service.HistorikkService.*;
import static no.nav.syfo.service.VeilederTilgangService.*;
import static no.nav.syfo.util.AuthorizationFilterUtils.basicCredentials;
import static no.nav.syfo.util.OidcTestHelper.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.ExpectedCount.manyTimes;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.web.util.UriComponentsBuilder.fromHttpUrl;

/**
 * Komponent / blackbox test av møtebehovsfunskjonaliteten - test at input til endepunktet (controlleren, for enkelhets skyld)
 * lagres og hentes riktig fra minnedatabasen.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = LocalApplication.class)
@DirtiesContext
public class MotebehovVeilederComponentTest {

    private static final String ARBEIDSTAKER_FNR = "12345678912";
    private static final String ARBEIDSTAKER_AKTOERID = AktoerMock.mockAktorId(ARBEIDSTAKER_FNR);
    private static final String LEDER_FNR = "10987654321";
    private static final String LEDER_AKTORID = AktoerMock.mockAktorId(LEDER_FNR);
    private static final String VIRKSOMHETSNUMMER = "1234";
    private static final String NAV_ENHET = "0330";
    private static final String VEILEDER_ID = "Z999999";
    private static final String NAVN = "Sygve Sykmeldt";
    private static final String BEDRIFT_NAVN = "NAV AS";
    private static final String HISTORIKK_SIST_ENDRET = "2018-10-10";

    @Value("${tilgangskontrollapi.url}")
    private String tilgangskontrollUrl;

    @Value("${syfoveilederoppgaver_system_v1.url}")
    private String syfoveilederoppgaverUrl;

    @Value("${syfoveilederoppgaver.systemapi.username}")
    private String credUsername;

    @Value("${syfoveilederoppgaver.systemapi.password}")
    private String credPassword;

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
        NyttMotebehov nyttMotebehov = arbeidsgiverLagrerMotebehov(LEDER_FNR, ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER);

        // Veileder henter møtebehov
        loggInnVeileder(oidcRequestContextHolder, VEILEDER_ID);
        mockSvarFraSyfoTilgangskontroll(ARBEIDSTAKER_FNR, OK);

        List<Motebehov> motebehovListe = motebehovVeilederController.hentMotebehovListe(ARBEIDSTAKER_FNR);
        assertThat(motebehovListe).size().isOne();

        Motebehov motebehov = motebehovListe.get(0);
        assertThat(motebehov.opprettetAv).isEqualTo(LEDER_AKTORID);
        assertThat(motebehov.arbeidstakerFnr).isEqualTo(ARBEIDSTAKER_FNR);
        assertThat(motebehov.virksomhetsnummer).isEqualTo(VIRKSOMHETSNUMMER);
        assertThat(motebehov.motebehovSvar).isEqualToComparingFieldByField(nyttMotebehov.motebehovSvar);
    }

    @Test
    public void sykmeldtLagrerOgVeilederHenterMotebehov() {
        NyttMotebehov nyttMotebehov = sykmeldtLagrerMotebehov(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER);

        // Veileder henter møtebehov
        loggInnVeileder(oidcRequestContextHolder, VEILEDER_ID);
        mockSvarFraSyfoTilgangskontroll(ARBEIDSTAKER_FNR, OK);

        List<Motebehov> motebehovListe = motebehovVeilederController.hentMotebehovListe(ARBEIDSTAKER_FNR);
        assertThat(motebehovListe).size().isOne();

        Motebehov motebehov = motebehovListe.get(0);
        assertThat(motebehov.opprettetAv).isEqualTo(ARBEIDSTAKER_AKTOERID);
        assertThat(motebehov.arbeidstakerFnr).isEqualTo(ARBEIDSTAKER_FNR);
        assertThat(motebehov.virksomhetsnummer).isEqualTo(VIRKSOMHETSNUMMER);
        assertThat(motebehov.motebehovSvar).isEqualToComparingFieldByField(nyttMotebehov.motebehovSvar);
    }

    @Test
    public void hentSykmeldteMedMotebehovSvarPaaEnhet() throws Exception {
        sykmeldtLagrerMotebehov(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER);

        loggInnVeileder(oidcRequestContextHolder, VEILEDER_ID);
        mockSvarFraSyfoTilgangsKontrollPaaEnhet(NAV_ENHET, OK);

        List<String> sykmeldteMedMotebehovPaaEnhet = motebehovVeilederController.hentSykmeldteMedMotebehovSvarPaaEnhet(NAV_ENHET);
        String sykmeldt = sykmeldteMedMotebehovPaaEnhet.get(0);

        assertThat(sykmeldt).isEqualTo(ARBEIDSTAKER_FNR);
    }

    @Test
    public void hentHistorikk() throws Exception {
        arbeidsgiverLagrerMotebehov(LEDER_FNR, ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER);

        loggInnVeileder(oidcRequestContextHolder, VEILEDER_ID);
        mockSvarFraSyfoTilgangsKontrollOgVeilederoppgaver(ARBEIDSTAKER_FNR, OK);

        List<Motebehov> motebehovListe = motebehovVeilederController.hentMotebehovListe(ARBEIDSTAKER_FNR);
        Motebehov motebehov = motebehovListe.get(0);

        List<Historikk> historikkListe = motebehovVeilederController.hentMotebehovHistorikk(ARBEIDSTAKER_FNR);
        assertThat(historikkListe).size().isEqualTo(3);

        Historikk motebehovOpprettetHistorikk = historikkListe.get(0);
        assertThat(motebehovOpprettetHistorikk.opprettetAv).isEqualTo(LEDER_AKTORID);
        assertThat(motebehovOpprettetHistorikk.tekst).isEqualTo(NAVN + HAR_SVART_PAA_MOTEBEHOV);
        assertThat(motebehovOpprettetHistorikk.tidspunkt).isEqualTo(motebehov.opprettetDato);

        Historikk veilederOppgaveHistorikk = historikkListe.get(1);
        assertThat(veilederOppgaveHistorikk.tekst).isEqualTo(MOTEBEHOVET_BLE_LEST_AV + VEILEDER_ID);
        assertThat(veilederOppgaveHistorikk.tidspunkt).isEqualTo(LocalDateTime.of(2018, 10, 10, 0, 0));

        Historikk varselHistorikk = historikkListe.get(2);
        assertThat(varselHistorikk.tekst).isEqualTo(VARSEL_OM_MOTEBEHOV_SENDT_LEDER + BEDRIFT_NAVN);
        assertThat(varselHistorikk.tidspunkt).isEqualTo(LocalDateTime.of(2018, 10, 10, 0, 0));
    }

    private NyttMotebehov arbeidsgiverLagrerMotebehov(String lederFnr, String arbeidstakerFnr, String virksomhetsnummer) {
        loggInnBruker(oidcRequestContextHolder, lederFnr);
        final MotebehovSvar motebehovSvar = new MotebehovSvar()
                .harMotebehov(true)
                .friskmeldingForventning("Om en uke")
                .tiltak("Krykker")
                .tiltakResultat("Kommer seg fremover")
                .forklaring("");

        final NyttMotebehov nyttMotebehov = new NyttMotebehov()
                .arbeidstakerFnr(arbeidstakerFnr)
                .virksomhetsnummer(virksomhetsnummer)
                .motebehovSvar(
                        motebehovSvar
                );

        motebehovController.lagreMotebehov(nyttMotebehov);

        return nyttMotebehov;
    }

    private NyttMotebehov sykmeldtLagrerMotebehov(String sykmeldtFnr, String virksomhetsnummer) {
        loggInnBruker(oidcRequestContextHolder, sykmeldtFnr);
        final MotebehovSvar motebehovSvar = new MotebehovSvar()
                .harMotebehov(true)
                .friskmeldingForventning("Om noen uker")
                .tiltak("Krykker")
                .tiltakResultat("Kommer seg fremover")
                .forklaring("");

        final NyttMotebehov nyttMotebehov = new NyttMotebehov()
                .arbeidstakerFnr(sykmeldtFnr)
                .virksomhetsnummer(virksomhetsnummer)
                .motebehovSvar(
                        motebehovSvar
                );

        motebehovController.lagreMotebehov(nyttMotebehov);

        return nyttMotebehov;
    }


    private void mockSvarFraSyfoTilgangskontroll(String fnr, HttpStatus status) {
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

    private void mockSvarFraSyfoTilgangsKontrollPaaEnhet(String enhet, HttpStatus status) throws Exception {
        String uriString = fromHttpUrl(tilgangskontrollUrl)
                .path(TILGANG_TIL_ENHET_PATH)
                .queryParam(ENHET, enhet)
                .toUriString();

        String idToken = oidcRequestContextHolder.getOIDCValidationContext().getToken("intern").getIdToken();

        mockRestServiceServer.expect(once(), requestTo(uriString))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(AUTHORIZATION, "Bearer " + idToken))
                .andRespond(withStatus(status));
    }

    private void mockSvarFraSyfoTilgangsKontrollOgVeilederoppgaver(String fnr, HttpStatus status) throws Exception {
        String uriString = fromHttpUrl(tilgangskontrollUrl)
                .path(TILGANG_TIL_BRUKER_PATH)
                .queryParam(FNR, fnr)
                .toUriString();

        String idToken = oidcRequestContextHolder.getOIDCValidationContext().getToken("intern").getIdToken();

        mockRestServiceServer.expect(manyTimes(), requestTo(uriString))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(AUTHORIZATION, "Bearer " + idToken))
                .andRespond(withStatus(status));

        List<VeilederOppgave> oppgaveListe = singletonList(
                new VeilederOppgave()
                        .id(1L)
                        .type("MOTEBEHOV_MOTTATT")
                        .tildeltIdent(VEILEDER_ID)
                        .tildeltEnhet(NAV_ENHET)
                        .lenke("123")
                        .fnr(fnr)
                        .virksomhetsnummer(VIRKSOMHETSNUMMER)
                        .created(HISTORIKK_SIST_ENDRET)
                        .sistEndret(HISTORIKK_SIST_ENDRET)
                        .sistEndretAv(VEILEDER_ID)
                        .status("FERDIG")
                        .uuid("000000")
        );

        String oppgaveListeJson = new ObjectMapper().writeValueAsString(oppgaveListe);

        String veilederoppgaverUriString = fromHttpUrl(syfoveilederoppgaverUrl)
                .queryParam(FNR, fnr)
                .toUriString();

        mockRestServiceServer.expect(once(), requestTo(veilederoppgaverUriString))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(AUTHORIZATION, basicCredentials(credUsername, credPassword)))
                .andRespond(withSuccess(oppgaveListeJson, APPLICATION_JSON));
    }

    private void cleanDB() {
        motebehovDAO.nullstillMotebehov(ARBEIDSTAKER_AKTOERID);
    }

}
