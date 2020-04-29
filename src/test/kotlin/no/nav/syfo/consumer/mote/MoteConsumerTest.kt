package no.nav.syfo.consumer.mote

import junit.framework.TestCase
import no.nav.syfo.metric.Metric
import no.nav.syfo.consumer.sts.StsConsumer
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
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import java.time.LocalDateTime

@RunWith(MockitoJUnitRunner::class)
class MoteConsumerTest {
    @Mock
    private lateinit var stsConsumer: StsConsumer

    @Mock
    private lateinit var metric: Metric

    @Mock
    private lateinit var template: RestTemplate

    @InjectMocks
    private lateinit var moteConsumer: MoteConsumer

    private val START_DATO = LocalDateTime.now().minusDays(30)

    @Before
    fun setUp() {
        Mockito.`when`(stsConsumer.token()).thenReturn("token")
    }

    @Test
    fun harArbeidstakerMoteIOppfolgingstilfelle_skal_returnere_true_hvis_moteplanlegger_er_brukt() {
        Mockito.`when`(template.postForObject(ArgumentMatchers.anyString(), ArgumentMatchers.any(HttpEntity::class.java), ArgumentMatchers.any<Class<Any>>())).thenReturn(true)
        Assert.assertTrue(moteConsumer.erMoteOpprettetForArbeidstakerEtterDato(ARBEIDSTAKER_AKTORID, START_DATO))
    }

    @Test
    fun harArbeidstakerMoteIOppfolgingstilfelle_skal_returnere_false_hvis_moteplanlegger_ikke_er_brukt() {
        Mockito.`when`(template.postForObject(ArgumentMatchers.anyString(), ArgumentMatchers.any(HttpEntity::class.java), ArgumentMatchers.any<Class<Any>>())).thenReturn(false)
        TestCase.assertFalse(moteConsumer.erMoteOpprettetForArbeidstakerEtterDato(ARBEIDSTAKER_AKTORID, START_DATO))
    }

    @Test(expected = HttpServerErrorException::class)
    fun harArbeidstakerMoteIOppfolgingstilfelle_skal_kaste_HttpServerErrorException_hvis_kall_til_syfomoteadmin_feiler() {
        Mockito.`when`(template.postForObject(ArgumentMatchers.anyString(), ArgumentMatchers.any(HttpEntity::class.java), ArgumentMatchers.any<Class<Any>>())).thenThrow(HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR))
        moteConsumer.erMoteOpprettetForArbeidstakerEtterDato(ARBEIDSTAKER_AKTORID, START_DATO)
    }
}
