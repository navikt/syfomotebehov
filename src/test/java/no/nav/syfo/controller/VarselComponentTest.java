package no.nav.syfo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.syfo.LocalApplication;
import no.nav.syfo.domain.rest.MotebehovsvarVarselInfo;
import no.nav.syfo.kafka.producer.TredjepartsvarselProducer;
import no.nav.syfo.kafka.producer.model.KTredjepartsvarsel;
import no.nav.syfo.service.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
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
import javax.ws.rs.core.Response;
import java.time.LocalDate;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static no.nav.syfo.kafka.producer.VarselType.NAERMESTE_LEDER_SVAR_MOTEBEHOV;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.web.util.UriComponentsBuilder.fromHttpUrl;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = LocalApplication.class)
@DirtiesContext
public class VarselComponentTest {

    @Value("${syfosyketilfelleApi.url}")
    String syfosyketilfelleUrl;

    @Value("${syfomoteadminapi.url}")
    String syfomoteadminUrl;

    @Inject
    private VarselController varselController;

    @Inject
    private RestTemplate restTemplate;

    @Inject
    private MoterService moterService;

    @Inject
    private VarselService varselService;

    @Inject
    private SyketilfelleService syketilfelleService;

    @Inject
    private TredjepartsvarselProducer tredjepartsvarselProducer;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    private MockRestServiceServer mockRestServiceServer;

    private ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(WRITE_DATES_AS_TIMESTAMPS, false);

    private final String AKTOR_ID = "123";

    private final String ORGNUMMER = "999";

    private LocalDate oppfolgingstilfelleStartDato = LocalDate.now().minusDays(30);

    private MotebehovsvarVarselInfo motebehovsvarVarselInfo = new MotebehovsvarVarselInfo()
            .sykmeldtAktorId(AKTOR_ID)
            .orgnummer(ORGNUMMER);

    private ArgumentCaptor<KTredjepartsvarsel> argumentCaptor = ArgumentCaptor.forClass(KTredjepartsvarsel.class);

    @Before
    public void setUp() {
        this.mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @After
    public void tearDown() {
        // Verify all expectations met
        mockRestServiceServer.verify();
    }

    @Test
    public void sendVarselNaermesteLeder_skal_sende_varsel_til_NL_hvis_ikke_mote() throws Exception {
        mockSvarFraSyfomoteadmin(false);
        when(kafkaTemplate.send(anyString(), anyString(), any(KTredjepartsvarsel.class))).thenReturn(mock(ListenableFuture.class));

        Response returnertSvarFraVarselcontroller = varselController.sendVarselNaermesteLeder(motebehovsvarVarselInfo);

        verify(kafkaTemplate).send(eq(tredjepartsvarselProducer.TREDJEPARTSVARSEL_TOPIC), anyString(), argumentCaptor.capture());

        KTredjepartsvarsel sendtKTredjepartsvarsel = argumentCaptor.getValue();
        verifySendtKtredjepartsvarsel(sendtKTredjepartsvarsel);

        assertEquals(HttpStatus.OK.value(), returnertSvarFraVarselcontroller.getStatus());
    }

    @Test
    public void sendVarselNaermesteLeder_skal_ikke_sende_varsel_til_NL_hvis_mote_finnes() throws Exception {
        mockSvarFraSyfomoteadmin(true);

        Response returnertSvarFraVarselcontroller = varselController.sendVarselNaermesteLeder(motebehovsvarVarselInfo);

        verify(kafkaTemplate, never()).send(any(), any(), any());
        assertEquals(HttpStatus.OK.value(), returnertSvarFraVarselcontroller.getStatus());
    }

    private void mockSvarFraSyfomoteadmin(boolean harAktivtMote) throws Exception {
        String svarFraSyfomoteadminJson = objectMapper.writeValueAsString(harAktivtMote);

        String url = fromHttpUrl(syfomoteadminUrl)
                .pathSegment("system", AKTOR_ID, "harAktivtMote")
                .toUriString();

        mockRestServiceServer.expect(once(), requestTo(url))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(svarFraSyfomoteadminJson, APPLICATION_JSON));

    }

    private void verifySendtKtredjepartsvarsel(KTredjepartsvarsel kTredjepartsvarsel) {
        assertEquals(kTredjepartsvarsel.getType(), NAERMESTE_LEDER_SVAR_MOTEBEHOV.name());
        assertNotNull(kTredjepartsvarsel.getRessursId());
        assertEquals(kTredjepartsvarsel.getAktorId(), AKTOR_ID);
        assertEquals(kTredjepartsvarsel.getOrgnummer(), ORGNUMMER);
        assertNotNull(kTredjepartsvarsel.getUtsendelsestidspunkt());
    }
}
