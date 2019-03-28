package no.nav.syfo.service;

import no.nav.syfo.consumer.ws.*;
import no.nav.syfo.domain.rest.BrukerPaaEnhet;
import no.nav.syfo.repository.dao.MotebehovDAO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static no.nav.syfo.domain.rest.BrukerPaaEnhet.Skjermingskode.DISKRESJONSMERKET;
import static no.nav.syfo.domain.rest.BrukerPaaEnhet.Skjermingskode.EGEN_ANSATT;
import static no.nav.syfo.domain.rest.BrukerPaaEnhet.Skjermingskode.INGEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MotebehovServiceTest {

    @Mock
    private AktoerConsumer aktoerConsumer;

    @Mock
    private PersonConsumer personConsumer;

    @Mock
    private EgenAnsattConsumer egenAnsattConsumer;

    @Mock
    private MotebehovDAO motebehovDAO;

    @InjectMocks
    private MotebehovService motebehovService;

    private static final String SM_AKTORID = "SM_AKTORID";
    private static final String SM_KODE6_AKTORID = "SM_KODE6_AKTORID";
    private static final String SM_EGENANSATT_AKTORID = "SM_EGENANSATT_AKTORID";
    private static final String SM_FNR = "SM_FNR";
    private static final String SM_KODE6_FNR = "SM_KODE6_FNR";
    private static final String SM_EGENANSATT_FNR = "SM_EGENANSATT_FNR";
    private static final String ENHET = "ENHET";
    private static final Optional<List<String>> MOTEBEHOV_PAA_ENHET = Optional.of(asList(SM_AKTORID, SM_KODE6_AKTORID, SM_EGENANSATT_AKTORID));

    @Before
    public void setup() {
        when(aktoerConsumer.hentFnrForAktoerId(SM_AKTORID)).thenReturn(SM_FNR);
        when(aktoerConsumer.hentFnrForAktoerId(SM_KODE6_AKTORID)).thenReturn(SM_KODE6_FNR);
        when(aktoerConsumer.hentFnrForAktoerId(SM_EGENANSATT_AKTORID)).thenReturn(SM_EGENANSATT_FNR);

        when(aktoerConsumer.hentAktoerIdForFnr(SM_FNR)).thenReturn(SM_AKTORID);
        when(aktoerConsumer.hentAktoerIdForFnr(SM_KODE6_FNR)).thenReturn(SM_KODE6_AKTORID);
        when(aktoerConsumer.hentAktoerIdForFnr(SM_EGENANSATT_FNR)).thenReturn(SM_EGENANSATT_AKTORID);

        when(personConsumer.erBrukerDiskresjonsmerket(SM_AKTORID)).thenReturn(false);
        when(egenAnsattConsumer.erEgenAnsatt(SM_FNR)).thenReturn(false);

        when(personConsumer.erBrukerDiskresjonsmerket(SM_KODE6_AKTORID)).thenReturn(true);

        when(personConsumer.erBrukerDiskresjonsmerket(SM_EGENANSATT_AKTORID)).thenReturn(false);
        when(egenAnsattConsumer.erEgenAnsatt(SM_EGENANSATT_FNR)).thenReturn(true);

        when(motebehovDAO.hentAktorIdMedMotebehovForEnhet(ENHET)).thenReturn(MOTEBEHOV_PAA_ENHET);
    }

    @Test
    public void hentSykmeldteMedMotebehovPaaEnhetSkalReturnereBrukereMedRiktigSkjermingskode() {
        List<BrukerPaaEnhet> brukerePaaEnhet = motebehovService.hentSykmeldteMedMotebehovPaaEnhet(ENHET);
        assertThat(brukerePaaEnhet.size()).isEqualTo(3);
        assertThat(brukerePaaEnhet.get(0).fnr).isEqualTo(SM_FNR);
        assertThat(brukerePaaEnhet.get(0).skjermetEllerEgenAnsatt).isEqualTo(INGEN);
        assertThat(brukerePaaEnhet.get(1).fnr).isEqualTo(SM_KODE6_FNR);
        assertThat(brukerePaaEnhet.get(1).skjermetEllerEgenAnsatt).isEqualTo(DISKRESJONSMERKET);
        assertThat(brukerePaaEnhet.get(2).fnr).isEqualTo(SM_EGENANSATT_FNR);
        assertThat(brukerePaaEnhet.get(2).skjermetEllerEgenAnsatt).isEqualTo(EGEN_ANSATT);
    }

}
