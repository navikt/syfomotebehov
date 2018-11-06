package no.nav.syfo.service;

import no.nav.syfo.OIDCIssuer;
import no.nav.syfo.consumer.ws.*;
import no.nav.syfo.domain.rest.*;
import no.nav.syfo.mappers.domain.Brukeroppgave;
import no.nav.syfo.mappers.domain.Hendelse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static java.time.LocalDateTime.now;

@RunWith(MockitoJUnitRunner.class)
public class HistorikkServiceTest {

    @Mock
    private AktoerConsumer aktoerConsumer;
    @Mock
    private BrukeroppgaveConsumer brukeroppgaveConsumer;
    @Mock
    private SykefravaeroppfoelgingConsumer sykefravaeroppfoelgingConsumer;
    @Mock
    private OrganisasjonConsumer organisasjonConsumer;
    @Mock
    private PersonConsumer personConsumer;
    @Mock
    private MotebehovService motebehovService;
    @Mock
    private VeilederOppgaverService veilederOppgaverService;
    @InjectMocks
    private HistorikkService historikkService;

    private static final String SM_FNR = "10000000001";
    private static final String NL1_FNR = "20000000001";
    private static final String NL2_FNR = "20000000002";
    private static final String NL3_FNR = "20000000003";

    private static final String SM_AKTORID = "1000000000001";
    private static final String NL1_AKTORID = "2000000000001";
    private static final String NL2_AKTORID = "2000000000002";
    private static final String NL3_AKTORID = "2000000000003";
    private static final String NL1_NAVN = "NL1 navn";
    private static final String NL3_NAVN = "NL3 navn";

    private static final String ORGNR_1 = "123456789";
    private static final String ORGNR_2 = "234567890";
    private static final String ORGNR_3 = "345678901";
    private static final String ORGNAVN_1 = "Bedrift 1";
    private static final String ORGNAVN_2 = "Bedrift 2";

    private static final String SVAR_MOTEBEHOV = "SVAR_MOTEBEHOV";
    private static final String AKTIVITETKRAV_VARSEL = "AKTIVITETKRAV_VARSEL";
    private static final String NAERMESTE_LEDER_SVAR_MOTEBEHOV = "NAERMESTE_LEDER_SVAR_MOTEBEHOV";
    private static final String NAERMESTE_LEDER_LES_SYKMELDING = "NAERMESTE_LEDER_LES_SYKMELDING";

    private static final String Z_IDENT1 = "ZIdent1";
    private static final String Z_IDENT2 = "ZIdent2";
    private static final String Z_IDENT3 = "ZIdent3";

    @Before
    public void setup() {
        when(aktoerConsumer.hentAktoerIdForFnr(SM_FNR)).thenReturn(SM_AKTORID);
        when(organisasjonConsumer.hentBedriftnavn(ORGNR_2)).thenReturn(ORGNAVN_2);
        when(personConsumer.hentNavnFraAktoerId(NL1_AKTORID)).thenReturn(NL1_NAVN);
        when(personConsumer.hentNavnFraAktoerId(NL3_AKTORID)).thenReturn(NL3_NAVN);
    }

    @Test
    public void hentHistorikkServiceSkalReturnereHistorikkAvForskjelligeTyper() {
        when(sykefravaeroppfoelgingConsumer.hentNaermesteLedere(SM_AKTORID, OIDCIssuer.INTERN)).thenReturn(asList(
                new NaermesteLeder()
                        .naermesteLederId(1111L)
                        .naermesteLederAktoerId(NL1_AKTORID)
                        .orgnummer(ORGNR_1)
                        .naermesteLederStatus(new NaermesteLederStatus()
                                .erAktiv(true)
                        ),
                new NaermesteLeder()
                        .naermesteLederId(2222L)
                        .naermesteLederAktoerId(NL2_AKTORID)
                        .orgnummer(ORGNR_2)
                        .naermesteLederStatus(new NaermesteLederStatus()
                                .erAktiv(true)
                        ),
                new NaermesteLeder()
                        .naermesteLederId(3333L)
                        .naermesteLederAktoerId(NL3_AKTORID)
                        .orgnummer(ORGNR_3)
                        .naermesteLederStatus(new NaermesteLederStatus()
                                .erAktiv(true)
                        )
        ));
        when(sykefravaeroppfoelgingConsumer.hentHendelserForSykmeldt(SM_AKTORID, OIDCIssuer.INTERN)).thenReturn(asList(
                new Hendelse()
                        .hendelseId(1)
                        .tidspunkt(now())
                        .type(SVAR_MOTEBEHOV)
                        .aktorId(SM_AKTORID),
                new Hendelse()
                        .hendelseId(2)
                        .tidspunkt(now())
                        .type(AKTIVITETKRAV_VARSEL)
                        .aktorId(SM_AKTORID)
        ));
        when(brukeroppgaveConsumer.hentBrukerOppgaver(NL1_AKTORID)).thenReturn(asList(
                new Brukeroppgave()
                        .ressursId("4")
                        .oppgavetype(NAERMESTE_LEDER_SVAR_MOTEBEHOV),
                new Brukeroppgave()
                        .ressursId("2")
                        .oppgavetype(NAERMESTE_LEDER_LES_SYKMELDING)
        ));
        when(brukeroppgaveConsumer.hentBrukerOppgaver(NL2_AKTORID)).thenReturn(asList(
                new Brukeroppgave()
                        .ressursId("1")
                        .oppgavetype(NAERMESTE_LEDER_SVAR_MOTEBEHOV)
        ));
        when(motebehovService.hentMotebehovListe(any(Fnr.class))).thenReturn(asList(
                new Motebehov()
                        .opprettetAv(NL3_AKTORID)
                        .opprettetDato(now()),
                new Motebehov()
                        .opprettetAv(NL1_AKTORID)
                        .opprettetDato(now().minusMinutes(2L))
        ));
        when(veilederOppgaverService.get(SM_FNR)).thenReturn(asList(
                new VeilederOppgave()
                        .type("SE_OPPFOLGINGSPLAN")
                        .status("IKKE_STARTET")
                        .sistEndretAv(Z_IDENT1)
                        .sistEndret("2018-01-01"),
                new VeilederOppgave()
                        .type("MOTEBEHOV_MOTTATT")
                        .status("FERDIG")
                        .sistEndretAv(Z_IDENT2)
                        .sistEndret("2018-01-02"),
                new VeilederOppgave()
                        .type("MOTEBEHOV_MOTTATT")
                        .status("IKKE_STARTET")
                        .sistEndretAv(Z_IDENT3)
                        .sistEndret("2018-01-03")
        ));
        List<Historikk> historikkForSykmeldt = historikkService.hentHistorikkListe(Fnr.of(SM_FNR));

        assertThat(historikkForSykmeldt.size()).isEqualTo(4);

        String historikkOpprettetMotebehovTekst1 = historikkForSykmeldt.get(0).tekst();
        String historikkOpprettetMotebehovTekst2 = historikkForSykmeldt.get(1).tekst();
        String historikkLesteMotebehovTekst = historikkForSykmeldt.get(2).tekst();
        String historikkVarselTilNLTekst = historikkForSykmeldt.get(3).tekst();

        assertThat(historikkOpprettetMotebehovTekst1).contains("Møtebehovet ble opprettet av");
        assertThat(historikkOpprettetMotebehovTekst1).contains(NL3_NAVN);

        assertThat(historikkOpprettetMotebehovTekst2).contains("Møtebehovet ble opprettet av");
        assertThat(historikkOpprettetMotebehovTekst2).contains(NL1_NAVN);

        assertThat(historikkLesteMotebehovTekst).contains("Møtebehovet ble lest av");
        assertThat(historikkLesteMotebehovTekst).contains(Z_IDENT2);

        assertThat(historikkVarselTilNLTekst).contains("Varsel om svar");
        assertThat(historikkVarselTilNLTekst).contains(ORGNAVN_2);
    }

}
