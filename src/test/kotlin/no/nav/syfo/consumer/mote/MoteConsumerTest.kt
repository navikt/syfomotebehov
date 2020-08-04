package no.nav.syfo.consumer.mote

import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.consumer.sts.StsConsumer
import no.nav.syfo.metric.Metric
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_AKTORID
import org.junit.jupiter.api.*
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import java.time.LocalDateTime

class MoteConsumerTest {
    private val metric = mockk<Metric>()
    private val stsConsumer = mockk<StsConsumer>()
    private val template = mockk<RestTemplate>()

    private val moteConsumer = MoteConsumer(
        stsConsumer = stsConsumer,
        metric = metric,
        template = template
    )

    private val startDate = LocalDateTime.now().minusDays(30)

    @BeforeEach
    fun setUp() {
        every { stsConsumer.token() } returns "token"
    }

    @Test
    fun harArbeidstakerMoteIOppfolgingstilfelle_skal_returnere_true_hvis_moteplanlegger_er_brukt() {
        every { template.postForObject(any<String>(), any(), any<Class<Any>>()) } returns true
        Assertions.assertTrue(moteConsumer.erMoteOpprettetForArbeidstakerEtterDato(ARBEIDSTAKER_AKTORID, startDate))
    }

    @Test
    fun harArbeidstakerMoteIOppfolgingstilfelle_skal_returnere_false_hvis_moteplanlegger_ikke_er_brukt() {
        every { template.postForObject(any<String>(), any(), any<Class<Any>>()) } returns false
        Assertions.assertFalse(moteConsumer.erMoteOpprettetForArbeidstakerEtterDato(ARBEIDSTAKER_AKTORID, startDate))
    }

    @Test
    fun harArbeidstakerMoteIOppfolgingstilfelle_skal_kaste_HttpServerErrorException_hvis_kall_til_syfomoteadmin_feiler() {
        every { template.postForObject(any<String>(), any(), any<Class<Any>>()) }.throws(HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR))
        assertThrows<HttpServerErrorException> { moteConsumer.erMoteOpprettetForArbeidstakerEtterDato(ARBEIDSTAKER_AKTORID, startDate) }
    }
}
