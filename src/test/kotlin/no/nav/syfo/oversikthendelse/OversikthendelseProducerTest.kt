package no.nav.syfo.oversikthendelse

import no.nav.syfo.kafka.producer.model.KOversikthendelse
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testhelper.UserConstants.NAV_ENHET
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.util.concurrent.ListenableFuture
import java.time.LocalDateTime

@RunWith(MockitoJUnitRunner::class)
class OversikthendelseProducerTest {
    @Mock
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    @InjectMocks
    private lateinit var oversikthendelseProducer: OversikthendelseProducer

    @Test
    fun sendOversikthendelse() {
        Mockito.`when`(kafkaTemplate!!.send(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.any(KOversikthendelse::class.java))).thenReturn(Mockito.mock(ListenableFuture::class.java) as ListenableFuture<SendResult<String, Any>>?)
        val kOversikthendelse = KOversikthendelse()
                .fnr(ARBEIDSTAKER_FNR)
                .hendelseId(OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name)
                .enhetId(NAV_ENHET)
                .tidspunkt(LocalDateTime.now())
        oversikthendelseProducer.sendOversikthendelse(kOversikthendelse)
        Mockito.verify(kafkaTemplate).send(ArgumentMatchers.eq(OversikthendelseProducer.OVERSIKTHENDELSE_TOPIC), ArgumentMatchers.anyString(), ArgumentMatchers.same(kOversikthendelse))
    }

    @Test
    fun sendOversikthendelseMotebehovSvarBehandlet() {
        Mockito.`when`(kafkaTemplate.send(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.any(KOversikthendelse::class.java))).thenReturn(Mockito.mock(ListenableFuture::class.java) as ListenableFuture<SendResult<String, Any>>?)
        val kOversikthendelse = KOversikthendelse()
                .fnr(ARBEIDSTAKER_FNR)
                .hendelseId(OversikthendelseType.MOTEBEHOV_SVAR_BEHANDLET.name)
                .enhetId(NAV_ENHET)
                .tidspunkt(LocalDateTime.now())
        oversikthendelseProducer.sendOversikthendelse(kOversikthendelse)
        Mockito.verify(kafkaTemplate).send(ArgumentMatchers.eq(OversikthendelseProducer.OVERSIKTHENDELSE_TOPIC), ArgumentMatchers.anyString(), ArgumentMatchers.same(kOversikthendelse))
    }
}
