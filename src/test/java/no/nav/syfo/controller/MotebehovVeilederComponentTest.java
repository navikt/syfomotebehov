package no.nav.syfo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.syfo.LocalApplication;
import no.nav.syfo.aktorregister.AktorregisterConsumer;
import no.nav.syfo.aktorregister.domain.AktorId;
import no.nav.syfo.aktorregister.domain.Fodselsnummer;
import no.nav.syfo.behandlendeenhet.BehandlendeEnhet;
import no.nav.syfo.behandlendeenhet.BehandlendeEnhetConsumer;
import no.nav.syfo.domain.rest.*;
import no.nav.syfo.historikk.Historikk;
import no.nav.syfo.kafka.producer.model.KOversikthendelse;
import no.nav.syfo.pdl.PdlConsumer;
import no.nav.syfo.repository.dao.MotebehovDAO;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.List;

import static java.util.Collections.singletonList;
import static no.nav.syfo.historikk.HistorikkService.HAR_SVART_PAA_MOTEBEHOV;
import static no.nav.syfo.historikk.HistorikkService.MOTEBEHOVET_BLE_LEST_AV;
import static no.nav.syfo.kafka.producer.OversikthendelseProducer.OVERSIKTHENDELSE_TOPIC;
import static no.nav.syfo.service.VeilederTilgangService.FNR;
import static no.nav.syfo.service.VeilederTilgangService.TILGANG_TIL_BRUKER_PATH;
import static no.nav.syfo.testhelper.OidcTestHelper.*;
import static no.nav.syfo.testhelper.PdlPersonResponseGeneratorKt.generatePdlHentPerson;
import static no.nav.syfo.testhelper.RestHelperKt.mockAndExpectBrukertilgangRequest;
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

/**
 * Komponent / blackbox test av møtebehovsfunskjonaliteten - test at input til endepunktet (controlleren, for enkelhets skyld)
 * lagres og hentes riktig fra minnedatabasen.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = LocalApplication.class)
@DirtiesContext
public class MotebehovVeilederComponentTest {

    private static final String HISTORIKK_SIST_ENDRET = "2018-10-10";

    @Value("${tilgangskontrollapi.url}")
    private String tilgangskontrollUrl;

    @Value("${syfobrukertilgang.url}")
    private String brukertilgangUrl;

    @Value("${syfoveilederoppgaver.system.v1.url}")
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

    @MockBean
    private AktorregisterConsumer aktorregisterConsumer;
    @MockBean
    private PdlConsumer pdlConsumer;

    @MockBean
    private BehandlendeEnhetConsumer behandlendeEnhetConsumer;
    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    private MockRestServiceServer mockRestServiceServer;


    @Before
    public void setUp() {
        when(aktorregisterConsumer.getFnrForAktorId(new AktorId(ARBEIDSTAKER_AKTORID))).thenReturn(ARBEIDSTAKER_FNR);
        when(aktorregisterConsumer.getFnrForAktorId(new AktorId(LEDER_AKTORID))).thenReturn(LEDER_FNR);
        when(aktorregisterConsumer.getAktorIdForFodselsnummer(new Fodselsnummer(ARBEIDSTAKER_FNR))).thenReturn(ARBEIDSTAKER_AKTORID);
        when(aktorregisterConsumer.getAktorIdForFodselsnummer(new Fodselsnummer(LEDER_FNR))).thenReturn(LEDER_AKTORID);
        when(pdlConsumer.person(any())).thenReturn(generatePdlHentPerson(null, null));

        cleanDB();
        this.mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build();
        when(behandlendeEnhetConsumer.getBehandlendeEnhet(ARBEIDSTAKER_FNR)).thenReturn(new BehandlendeEnhet("", ""));
        when(kafkaTemplate.send(anyString(), anyString(), any(KOversikthendelse.class))).thenReturn(mock(ListenableFuture.class));
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
        mockAndExpectBrukertilgangRequest(mockRestServiceServer, brukertilgangUrl, ARBEIDSTAKER_FNR);
        NyttMotebehov nyttMotebehov = arbeidsgiverLagrerMotebehov(LEDER_FNR, ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER);

        // Veileder henter møtebehov
        loggInnVeileder(oidcRequestContextHolder, VEILEDER_ID);
        mockRestServiceServer.reset();
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
        NyttMotebehov nyttMotebehov = sykmeldtLagrerMotebehov(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER, true);

        // Veileder henter møtebehov
        loggInnVeileder(oidcRequestContextHolder, VEILEDER_ID);
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
        arbeidsgiverLagrerMotebehov(LEDER_FNR, ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER);

        mockRestServiceServer.reset();
        loggInnVeileder(oidcRequestContextHolder, VEILEDER_ID);
        mockSvarFraSyfoTilgangsKontrollOgVeilederoppgaver(ARBEIDSTAKER_FNR, OK);

        List<Motebehov> motebehovListe = motebehovVeilederController.hentMotebehovListe(ARBEIDSTAKER_FNR);
        Motebehov motebehov = motebehovListe.get(0);

        List<Historikk> historikkListe = motebehovVeilederController.hentMotebehovHistorikk(ARBEIDSTAKER_FNR);
        assertThat(historikkListe).size().isEqualTo(2);

        Historikk motebehovOpprettetHistorikk = historikkListe.get(0);
        assertThat(motebehovOpprettetHistorikk.getOpprettetAv()).isEqualTo(LEDER_AKTORID);
        assertThat(motebehovOpprettetHistorikk.getTekst()).isEqualTo(PERSON_FULL_NAME + HAR_SVART_PAA_MOTEBEHOV);
        assertThat(motebehovOpprettetHistorikk.getTidspunkt()).isEqualTo(motebehov.opprettetDato);

        Historikk veilederOppgaveHistorikk = historikkListe.get(1);
        assertThat(veilederOppgaveHistorikk.getTekst()).isEqualTo(MOTEBEHOVET_BLE_LEST_AV + VEILEDER_ID);
        assertThat(veilederOppgaveHistorikk.getTidspunkt()).isEqualTo(LocalDateTime.of(2018, 10, 10, 0, 0));
    }

    @Test
    public void hentMotebehovUbehandlet() {
        sykmeldtLagrerMotebehov(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER, true);
        mockAndExpectBrukertilgangRequest(mockRestServiceServer, brukertilgangUrl, ARBEIDSTAKER_FNR);
        arbeidsgiverLagrerMotebehov(LEDER_FNR, ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER);

        loggInnVeileder(oidcRequestContextHolder, VEILEDER_ID);
        mockRestServiceServer.reset();
        mockSvarFraSyfoTilgangskontroll(ARBEIDSTAKER_FNR, OK);

        List<Motebehov> motebehovListe = motebehovVeilederController.hentMotebehovListe(ARBEIDSTAKER_FNR);

        motebehovListe.forEach(motebehov -> {
            assertThat(motebehov.behandletTidspunkt).isNull();
            assertThat(motebehov.behandletVeilederIdent).isNull();
        });
    }

    @Test
    public void behandleKunMotebehovMedBehovForMote() {
        sykmeldtLagrerMotebehov(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER, false);

        mockAndExpectBrukertilgangRequest(mockRestServiceServer, brukertilgangUrl, ARBEIDSTAKER_FNR);
        arbeidsgiverLagrerMotebehov(LEDER_FNR, ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER);

        loggInnVeileder(oidcRequestContextHolder, VEILEDER_ID);
        mockRestServiceServer.reset();
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
    public void behandleMotebehovUlikVeilederBehandler() {
        sykmeldtLagrerMotebehov(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER, true);
        behandleMotebehov(ARBEIDSTAKER_AKTORID, VEILEDER_ID);

        mockAndExpectBrukertilgangRequest(mockRestServiceServer, brukertilgangUrl, ARBEIDSTAKER_FNR);
        arbeidsgiverLagrerMotebehov(LEDER_FNR, ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER);

        loggInnVeileder(oidcRequestContextHolder, VEILEDER_2_ID);
        mockRestServiceServer.reset();
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
    public void behandleIkkeEksiterendeMotebehov() {
        sykmeldtLagrerMotebehov(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER, true);
        behandleMotebehov(ARBEIDSTAKER_AKTORID, VEILEDER_ID);

        loggInnVeileder(oidcRequestContextHolder, VEILEDER_2_ID);
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
                .path(TILGANG_TIL_BRUKER_PATH)
                .queryParam(FNR, fnr)
                .toUriString();

        String idToken = oidcRequestContextHolder.getOIDCValidationContext().getToken("intern").getIdToken();

        mockRestServiceServer.expect(manyTimes(), requestTo(uriString))
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
        motebehovDAO.nullstillMotebehov(ARBEIDSTAKER_AKTORID);
    }

}
