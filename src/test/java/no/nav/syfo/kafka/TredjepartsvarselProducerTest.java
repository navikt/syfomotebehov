package no.nav.syfo.kafka;

import no.nav.syfo.varsel.TredjepartsvarselProducer;
import no.nav.syfo.kafka.producer.model.KTredjepartsvarsel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.concurrent.ListenableFuture;

import static java.time.LocalDateTime.now;
import static no.nav.syfo.kafka.producer.VarselType.NAERMESTE_LEDER_SVAR_MOTEBEHOV;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TredjepartsvarselProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private TredjepartsvarselProducer tredjepartsvarselProducer;

    @Test
    public void sendTredjepartsvarsel() {
        when(kafkaTemplate.send(anyString(), anyString(), any(KTredjepartsvarsel.class))).thenReturn(mock(ListenableFuture.class));

        KTredjepartsvarsel kTredjepartsvarsel = new KTredjepartsvarsel()
                .type(NAERMESTE_LEDER_SVAR_MOTEBEHOV.name())
                .ressursId("1")
                .aktorId("1010101010101")
                .orgnummer("123456789")
                .utsendelsestidspunkt(now().plusMinutes(5));

        tredjepartsvarselProducer.sendTredjepartsvarselvarsel(kTredjepartsvarsel);

        verify(kafkaTemplate).send(eq(TredjepartsvarselProducer.TREDJEPARTSVARSEL_TOPIC), anyString(), same(kTredjepartsvarsel));
    }
}
