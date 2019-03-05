package no.nav.syfo.service;

import no.nav.syfo.consumer.ws.*;
import no.nav.syfo.domain.rest.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static java.time.LocalDateTime.now;
import static java.util.Arrays.asList;
import static no.nav.syfo.service.HistorikkService.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HistorikkServiceTest {

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
        when(personConsumer.hentNavnFraAktoerId(NL1_AKTORID)).thenReturn(NL1_NAVN);
        when(personConsumer.hentNavnFraAktoerId(NL3_AKTORID)).thenReturn(NL3_NAVN);
    }

    @Test
    public void hentHistorikkServiceSkalReturnereHistorikkAvForskjelligeTyper() {
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

        assertThat(historikkForSykmeldt.size()).isEqualTo(3);

        String historikkOpprettetMotebehovTekst1 = historikkForSykmeldt.get(0).tekst();
        String historikkOpprettetMotebehovTekst2 = historikkForSykmeldt.get(1).tekst();
        String historikkLesteMotebehovTekst = historikkForSykmeldt.get(2).tekst();

        assertThat(historikkOpprettetMotebehovTekst1).isEqualTo(NL3_NAVN + HAR_SVART_PAA_MOTEBEHOV);

        assertThat(historikkOpprettetMotebehovTekst2).isEqualTo(NL1_NAVN + HAR_SVART_PAA_MOTEBEHOV);

        assertThat(historikkLesteMotebehovTekst).isEqualTo(MOTEBEHOVET_BLE_LEST_AV + Z_IDENT2);
    }

}
