package no.nav.syfo.mote

import junit.framework.TestCase
import no.nav.syfo.metric.Metrikk
import no.nav.syfo.sts.StsConsumer
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_AKTORID
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import java.time.LocalDateTime

@RunWith(MockitoJUnitRunner::class)
class MoterServiceTest {
    @Mock
    private lateinit var stsConsumer: StsConsumer

    @Mock
    private lateinit var metrikk: Metrikk

    @Mock
    private lateinit var template: RestTemplate

    @InjectMocks
    private lateinit var moterService: MoterService

    private val START_DATO = LocalDateTime.now().minusDays(30)

    @Before
    fun setUp() {
        Mockito.`when`(stsConsumer.token()).thenReturn("token")
    }

    @Test
    fun harArbeidstakerMoteIOppfolgingstilfelle_skal_returnere_true_hvis_moteplanlegger_er_brukt() {
        Mockito.`when`(template.postForObject(ArgumentMatchers.anyString(), ArgumentMatchers.any(HttpEntity::class.java), ArgumentMatchers.any<Class<Any>>())).thenReturn(true)
        Assert.assertTrue(moterService.erMoteOpprettetForArbeidstakerEtterDato(ARBEIDSTAKER_AKTORID, START_DATO))
    }

    @Test
    fun harArbeidstakerMoteIOppfolgingstilfelle_skal_returnere_false_hvis_moteplanlegger_ikke_er_brukt() {
        Mockito.`when`(template.postForObject(ArgumentMatchers.anyString(), ArgumentMatchers.any(HttpEntity::class.java), ArgumentMatchers.any<Class<Any>>())).thenReturn(false)
        TestCase.assertFalse(moterService.erMoteOpprettetForArbeidstakerEtterDato(ARBEIDSTAKER_AKTORID, START_DATO))
    }

    @Test(expected = HttpServerErrorException::class)
    fun harArbeidstakerMoteIOppfolgingstilfelle_skal_kaste_HttpServerErrorException_hvis_kall_til_syfomoteadmin_feiler() {
        Mockito.`when`(template.postForObject(ArgumentMatchers.anyString(), ArgumentMatchers.any(HttpEntity::class.java), ArgumentMatchers.any<Class<Any>>())).thenThrow(HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR))
        moterService.erMoteOpprettetForArbeidstakerEtterDato(ARBEIDSTAKER_AKTORID, START_DATO)
    }
}
