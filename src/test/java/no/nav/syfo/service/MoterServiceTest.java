package no.nav.syfo.service;

import no.nav.syfo.sts.StsConsumer;
import no.nav.syfo.util.Metrikk;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpServerErrorException;
import java.time.LocalDateTime;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static junit.framework.TestCase.assertFalse;
import static no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_AKTORID;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MoterServiceTest {

    @Mock
    private StsConsumer stsConsumer;

    @Mock
    private Metrikk metrikk;

    @Mock
    private RestTemplate template;

    @InjectMocks
    private MoterService moterService;

    private final LocalDateTime START_DATO = LocalDateTime.now().minusDays(30);

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(moterService, "syfomoteadminUrl", "https://www.kanskje.no");
    }

    @Test
    public void harArbeidstakerMoteIOppfolgingstilfelle_skal_returnere_true_hvis_moteplanlegger_er_brukt() {
        when(template.postForObject(anyString(), any(HttpEntity.class), any())).thenReturn(true);

        assertTrue(moterService.erMoteOpprettetForArbeidstakerEtterDato(ARBEIDSTAKER_AKTORID, START_DATO));
    }

    @Test
    public void harArbeidstakerMoteIOppfolgingstilfelle_skal_returnere_false_hvis_moteplanlegger_ikke_er_brukt() {
        when(template.postForObject(anyString(), any(HttpEntity.class), any())).thenReturn(false);

        assertFalse(moterService.erMoteOpprettetForArbeidstakerEtterDato(ARBEIDSTAKER_AKTORID, START_DATO));
    }

    @Test(expected = NullPointerException.class)
    public void harArbeidstakerMoteIOppfolgingstilfelle_skal_kaste_NullPointerException_hvis_svar_fra_syfomoteadmin_er_null() {
        when(template.postForObject(anyString(), any(HttpEntity.class), any())).thenReturn(null);

        moterService.erMoteOpprettetForArbeidstakerEtterDato(ARBEIDSTAKER_AKTORID, START_DATO);
    }

    @Test(expected = RuntimeException.class)
    public void harArbeidstakerMoteIOppfolgingstilfelle_skal_kaste_RuntimeException_hvis_kall_til_syfomoteadmin_feiler() {
        when(template.postForObject(anyString(), any(HttpEntity.class), any())).thenThrow(new HttpServerErrorException(INTERNAL_SERVER_ERROR));

        moterService.erMoteOpprettetForArbeidstakerEtterDato(ARBEIDSTAKER_AKTORID, START_DATO);
    }
}
