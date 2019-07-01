package no.nav.syfo.service;

import no.nav.syfo.domain.rest.OppfolgingstilfelleDTO;
import no.nav.syfo.domain.rest.PeriodeDTO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MoterServiceTest {

    @Mock
    private RestTemplate template;

    @InjectMocks
    private MoterService moterService;

    private final String AKTOR_ID = "123";
    private final OppfolgingstilfelleDTO TILFELLE = new OppfolgingstilfelleDTO()
            .antallBrukteDager(120)
            .oppbruktArbeidsgvierperiode(true)
            .arbeidsgiverperiode(new PeriodeDTO()
                    .fom(LocalDate.now().minusDays(119))
                    .tom(LocalDate.now().plusDays(1)));


    @Before
    public void setUp() {
        ReflectionTestUtils.setField(moterService, "syfomoteadminUrl", "https://www.kanskje.no");
    }

    @Test
    public void harArbeidstakerMoteIOppfolgingstilfelle_skal_returnere_true_hvis_moteplanlegger_er_brukt() {
        when(template.postForObject(anyString(), any(LocalDateTime.class), any())).thenReturn(true);

        assertTrue(moterService.erMoteOpprettetForArbeidstakerEtterDato(AKTOR_ID, TILFELLE));
    }

    @Test
    public void harArbeidstakerMoteIOppfolgingstilfelle_skal_returnere_false_hvis_moteplanlegger_ikke_er_brukt() {
        when(template.postForObject(anyString(), any(LocalDateTime.class), any())).thenReturn(false);

        assertFalse(moterService.erMoteOpprettetForArbeidstakerEtterDato(AKTOR_ID, TILFELLE));
    }

    @Test(expected = RuntimeException.class)
    public void harArbeidstakerMoteIOppfolgingstilfelle_skal_kaste_exception_hvis_svar_fra_syfomoteadmin_er_null() {
        when(template.postForObject(anyString(), any(LocalDateTime.class), any())).thenReturn(null);

        moterService.erMoteOpprettetForArbeidstakerEtterDato(AKTOR_ID, TILFELLE);
    }

    @Test(expected = RuntimeException.class)
    public void harArbeidstakerMoteIOppfolgingstilfelle_skal_kaste_exception_hvis_kall_til_syfomoteadmin_feiler() {
        when(template.postForObject(anyString(), any(LocalDateTime.class), any())).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);

        moterService.erMoteOpprettetForArbeidstakerEtterDato(AKTOR_ID, TILFELLE);
    }
}
