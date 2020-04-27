package no.nav.syfo.varsel

import no.nav.syfo.kafka.producer.model.KTredjepartsvarsel
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
class TredjepartsvarselProducerTest {
    @Mock
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    @InjectMocks
    private lateinit var tredjepartsvarselProducer: TredjepartsvarselProducer

    @Test
    fun sendTredjepartsvarsel() {
        Mockito.`when`(kafkaTemplate.send(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.any(KTredjepartsvarsel::class.java))).thenReturn(Mockito.mock(ListenableFuture::class.java) as ListenableFuture<SendResult<String, Any>>?)
        val kTredjepartsvarsel = KTredjepartsvarsel()
                .type(VarselType.NAERMESTE_LEDER_SVAR_MOTEBEHOV.name)
                .ressursId("1")
                .aktorId("1010101010101")
                .orgnummer("123456789")
                .utsendelsestidspunkt(LocalDateTime.now().plusMinutes(5))
        tredjepartsvarselProducer.sendTredjepartsvarselvarsel(kTredjepartsvarsel)
        Mockito.verify(kafkaTemplate).send(ArgumentMatchers.eq(TredjepartsvarselProducer.TREDJEPARTSVARSEL_TOPIC), ArgumentMatchers.anyString(), ArgumentMatchers.same(kTredjepartsvarsel))
    }
}
