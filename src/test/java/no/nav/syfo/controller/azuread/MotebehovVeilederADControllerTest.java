package no.nav.syfo.controller.azuread;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.syfo.LocalApplication;
import no.nav.syfo.controller.MotebehovBrukerController;
import no.nav.syfo.domain.rest.*;
import no.nav.syfo.kafka.producer.model.KOversikthendelse;
import no.nav.syfo.repository.dao.MotebehovDAO;
import no.nav.syfo.sts.StsConsumer;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.List;

import static java.util.Collections.singletonList;
import static no.nav.syfo.kafka.producer.OversikthendelseProducer.OVERSIKTHENDELSE_TOPIC;
import static no.nav.syfo.mock.PersonMock.PERSON_NAVN;
import static no.nav.syfo.oidc.OIDCIssuer.AZURE;
import static no.nav.syfo.service.HistorikkService.HAR_SVART_PAA_MOTEBEHOV;
import static no.nav.syfo.service.HistorikkService.MOTEBEHOVET_BLE_LEST_AV;
import static no.nav.syfo.service.VeilederTilgangService.FNR;
import static no.nav.syfo.service.VeilederTilgangService.TILGANG_TIL_BRUKER_VIA_AZURE_PATH;
import static no.nav.syfo.testhelper.OidcTestHelper.*;
import static no.nav.syfo.testhelper.RestHelperKt.*;
import static no.nav.syfo.testhelper.UserConstants.*;
import static no.nav.syfo.util.AuthorizationFilterUtils.basicCredentials;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.ExpectedCount.manyTimes;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.web.util.UriComponentsBuilder.fromHttpUrl;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = LocalApplication.class)
@DirtiesContext
public class MotebehovVeilederADControllerTest {

    private static final String HISTORIKK_SIST_ENDRET = "2018-10-10";

    @Value("${tilgangskontrollapi.url}")
    private String tilgangskontrollUrl;

    @Value("${syfobehandlendeenhet.url}")
    private String behandlendeenhetUrl;

    @Value("${syfobrukertilgang.url}")
    private String brukertilgangUrl;

    @Value("${security.token.service.rest.url}")
    private String stsUrl;

    @Value("${srv.username}")
    private String srvUsername;

    @Value("${srv.password}")
    private String srvPassword;

    @Value("${syfoveilederoppgaver.system.v1.url}")
    private String syfoveilederoppgaverUrl;

    @Value("${syfoveilederoppgaver.systemapi.username}")
    private String credUsername;

    @Value("${syfoveilederoppgaver.systemapi.password}")
    private String credPassword;

    @Inject
    private MotebehovBrukerController motebehovController;

    @Inject
    private MotebehovVeilederADController motebehovVeilederController;

    @Inject
    private OIDCRequestContextHolder oidcRequestContextHolder;

    @Inject
    private MotebehovDAO motebehovDAO;

    @Inject
    private StsConsumer stsConsumer;

    @Inject
    private CacheManager cacheManager;

    @Inject
    private RestTemplate restTemplate;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    private MockRestServiceServer mockRestServiceServer;

    @Before
    public void setUp() {
        cleanDB();
        this.mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build();
        when(kafkaTemplate.send(anyString(), anyString(), any(KOversikthendelse.class))).thenReturn(mock(ListenableFuture.class));
    }

    @After
    public void tearDown() {
        // Verify all expectations met
        mockRestServiceServer.verify();
        loggUtAlle(oidcRequestContextHolder);
        mockRestServiceServer.reset();
        cacheManager.getCacheNames()
                .forEach(cacheName -> cacheManager.getCache(cacheName).clear());
        cleanDB();
    }

    @Test
    public void arbeidsgiverLagrerOgVeilederHenterMotebehov() throws ParseException {
        mockAndExpectBrukertilgangRequest(mockRestServiceServer, brukertilgangUrl, ARBEIDSTAKER_FNR);
        mockBehandlendEnhet(ARBEIDSTAKER_FNR);

        NyttMotebehov nyttMotebehov = arbeidsgiverLagrerMotebehov(LEDER_FNR, ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER);

        // Veileder henter møtebehov
        mockRestServiceServer.reset();
        loggInnVeilederAzure(oidcRequestContextHolder, VEILEDER_ID);
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
    public void sykmeldtLagrerOgVeilederHenterMotebehov() throws ParseException {
        mockBehandlendEnhet(ARBEIDSTAKER_FNR);

        NyttMotebehov nyttMotebehov = sykmeldtLagrerMotebehov(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER, true);

        // Veileder henter møtebehov
        mockRestServiceServer.reset();
        loggInnVeilederAzure(oidcRequestContextHolder, VEILEDER_ID);
        mockSvarFraSyfoTilgangskontroll(ARBEIDSTAKER_FNR, OK);

        List<Motebehov> motebehovListe = motebehovVeilederController.hentMotebehovListe(ARBEIDSTAKER_FNR);
        assertThat(motebehovListe).size().isOne();

        Motebehov motebehov = motebehovListe.get(0);
        assertThat(motebehov.opprettetAv).isEqualTo(ARBEIDSTAKER_AKTORID);
        assertThat(motebehov.arbeidstakerFnr).isEqualTo(ARBEIDSTAKER_FNR);
        assertThat(motebehov.virksomhetsnummer).isEqualTo(VIRKSOMHETSNUMMER);
        assertThat(motebehov.motebehovSvar).isEqualToComparingFieldByField(nyttMotebehov.motebehovSvar);
    }

    @Test
    public void hentHistorikk() throws Exception {
        mockAndExpectBrukertilgangRequest(mockRestServiceServer, brukertilgangUrl, ARBEIDSTAKER_FNR);
        mockBehandlendEnhet(ARBEIDSTAKER_FNR);

        arbeidsgiverLagrerMotebehov(LEDER_FNR, ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER);

        mockRestServiceServer.reset();
        loggInnVeilederAzure(oidcRequestContextHolder, VEILEDER_ID);
        mockSvarFraSyfoTilgangsKontrollOgVeilederoppgaver(ARBEIDSTAKER_FNR, OK);

        List<Motebehov> motebehovListe = motebehovVeilederController.hentMotebehovListe(ARBEIDSTAKER_FNR);
        Motebehov motebehov = motebehovListe.get(0);

        List<Historikk> historikkListe = motebehovVeilederController.hentMotebehovHistorikk(ARBEIDSTAKER_FNR);
        assertThat(historikkListe).size().isEqualTo(2);

        Historikk motebehovOpprettetHistorikk = historikkListe.get(0);
        assertThat(motebehovOpprettetHistorikk.opprettetAv).isEqualTo(LEDER_AKTORID);
        assertThat(motebehovOpprettetHistorikk.tekst).isEqualTo(PERSON_NAVN + HAR_SVART_PAA_MOTEBEHOV);
        assertThat(motebehovOpprettetHistorikk.tidspunkt).isEqualTo(motebehov.opprettetDato);

        Historikk veilederOppgaveHistorikk = historikkListe.get(1);
        assertThat(veilederOppgaveHistorikk.tekst).isEqualTo(MOTEBEHOVET_BLE_LEST_AV + VEILEDER_ID);
        assertThat(veilederOppgaveHistorikk.tidspunkt).isEqualTo(LocalDateTime.of(2018, 10, 10, 0, 0));
    }

    @Test
    public void hentMotebehovUbehandlet() throws ParseException {
        mockBehandlendEnhet(ARBEIDSTAKER_FNR);
        sykmeldtLagrerMotebehov(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER, true);

        mockRestServiceServer.reset();
        mockAndExpectBrukertilgangRequest(mockRestServiceServer, brukertilgangUrl, ARBEIDSTAKER_FNR);
        arbeidsgiverLagrerMotebehov(LEDER_FNR, ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER);

        mockRestServiceServer.reset();
        loggInnVeilederAzure(oidcRequestContextHolder, VEILEDER_ID);
        mockSvarFraSyfoTilgangskontroll(ARBEIDSTAKER_FNR, OK);

        List<Motebehov> motebehovListe = motebehovVeilederController.hentMotebehovListe(ARBEIDSTAKER_FNR);

        motebehovListe.forEach(motebehov -> {
            assertThat(motebehov.behandletTidspunkt).isNull();
            assertThat(motebehov.behandletVeilederIdent).isNull();
        });
    }

    @Test
    public void behandleKunMotebehovMedBehovForMote() throws ParseException {
        mockBehandlendEnhet(ARBEIDSTAKER_FNR);

        sykmeldtLagrerMotebehov(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER, false);

        mockRestServiceServer.reset();
        mockAndExpectBrukertilgangRequest(mockRestServiceServer, brukertilgangUrl, ARBEIDSTAKER_FNR);
        arbeidsgiverLagrerMotebehov(LEDER_FNR, ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER);

        mockRestServiceServer.reset();
        loggInnVeilederAzure(oidcRequestContextHolder, VEILEDER_ID);
        mockSvarFraSyfoTilgangskontroll(ARBEIDSTAKER_FNR, OK);

        motebehovVeilederController.behandleMotebehov(ARBEIDSTAKER_FNR);

        List<Motebehov> motebehovListe = motebehovVeilederController.hentMotebehovListe(ARBEIDSTAKER_FNR);

        assertThat(motebehovListe.get(0).behandletTidspunkt).isNull();
        assertThat(motebehovListe.get(0).behandletVeilederIdent).isEqualTo(null);

        assertThat(motebehovListe.get(1).behandletTidspunkt).isNotNull();
        assertThat(motebehovListe.get(1).behandletVeilederIdent).isEqualTo(VEILEDER_ID);

        verify(kafkaTemplate, times(2)).send(eq(OVERSIKTHENDELSE_TOPIC), anyString(), any());
    }

    @Test
    public void behandleMotebehovUlikVeilederBehandler() throws ParseException {
        mockBehandlendEnhet(ARBEIDSTAKER_FNR);

        sykmeldtLagrerMotebehov(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER, true);
        behandleMotebehov(ARBEIDSTAKER_AKTORID, VEILEDER_ID);

        mockRestServiceServer.reset();
        mockAndExpectBrukertilgangRequest(mockRestServiceServer, brukertilgangUrl, ARBEIDSTAKER_FNR);
        arbeidsgiverLagrerMotebehov(LEDER_FNR, ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER);

        mockRestServiceServer.reset();
        loggInnVeilederAzure(oidcRequestContextHolder, VEILEDER_2_ID);
        mockSvarFraSyfoTilgangskontroll(ARBEIDSTAKER_FNR, OK);

        motebehovVeilederController.behandleMotebehov(ARBEIDSTAKER_FNR);

        List<Motebehov> motebehovListe1 = motebehovVeilederController.hentMotebehovListe(ARBEIDSTAKER_FNR);

        assertThat(motebehovListe1.get(0).behandletTidspunkt).isNotNull();
        assertThat(motebehovListe1.get(0).behandletVeilederIdent).isEqualTo(VEILEDER_ID);

        assertThat(motebehovListe1.get(1).behandletTidspunkt).isNotNull();
        assertThat(motebehovListe1.get(1).behandletVeilederIdent).isEqualTo(VEILEDER_2_ID);

        verify(kafkaTemplate, times(3)).send(eq(OVERSIKTHENDELSE_TOPIC), anyString(), any());
    }

    @Test(expected = RuntimeException.class)
    public void behandleIkkeEksiterendeMotebehov() throws ParseException {
        mockBehandlendEnhet(ARBEIDSTAKER_FNR);

        sykmeldtLagrerMotebehov(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER, true);
        behandleMotebehov(ARBEIDSTAKER_AKTORID, VEILEDER_ID);

        mockRestServiceServer.reset();
        loggInnVeilederAzure(oidcRequestContextHolder, VEILEDER_2_ID);
        mockSvarFraSyfoTilgangskontroll(ARBEIDSTAKER_FNR, OK);

        motebehovVeilederController.behandleMotebehov(ARBEIDSTAKER_FNR);
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

    private NyttMotebehov sykmeldtLagrerMotebehov(String sykmeldtFnr, String virksomhetsnummer, boolean harBehov) {
        loggInnBruker(oidcRequestContextHolder, sykmeldtFnr);
        final MotebehovSvar motebehovSvar = new MotebehovSvar()
                .harMotebehov(harBehov)
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

    private void behandleMotebehov(String aktoerId, String veileder) {
        motebehovDAO.oppdaterUbehandledeMotebehovTilBehandlet(aktoerId, veileder);
    }


    private void mockSvarFraSyfoTilgangskontroll(String fnr, HttpStatus status) {
        String uriString = fromHttpUrl(tilgangskontrollUrl)
                .path(TILGANG_TIL_BRUKER_VIA_AZURE_PATH)
                .queryParam(FNR, fnr)
                .toUriString();

        String idToken = oidcRequestContextHolder.getOIDCValidationContext().getToken(AZURE).getIdToken();

        mockRestServiceServer.expect(manyTimes(), requestTo(uriString))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(AUTHORIZATION, "Bearer " + idToken))
                .andRespond(withStatus(status));
    }

    private void mockSTS() {
        if (!stsConsumer.isTokenCached()) {
            mockAndExpectSTSService(mockRestServiceServer, stsUrl, srvUsername, srvPassword);
        }
    }

    private void mockBehandlendEnhet(String fnr) {
        mockSTS();
        mockAndExpectBehandlendeEnhetRequest(mockRestServiceServer, behandlendeenhetUrl, fnr);
    }

    private void mockSvarFraSyfoTilgangsKontrollOgVeilederoppgaver(String fnr, HttpStatus status) throws Exception {
        String uriString = fromHttpUrl(tilgangskontrollUrl)
                .path(TILGANG_TIL_BRUKER_VIA_AZURE_PATH)
                .queryParam(FNR, fnr)
                .toUriString();

        String idToken = oidcRequestContextHolder.getOIDCValidationContext().getToken(AZURE).getIdToken();

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
        motebehovDAO.nullstillMotebehov(ARBEIDSTAKER_AKTORID);
    }

}
