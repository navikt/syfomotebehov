package no.nav.syfo.kafka;

import no.nav.syfo.kafka.producer.TredjepartsVarselNokkel;
import no.nav.syfo.kafka.producer.TredjepartsvarselProducer;
import no.nav.syfo.kafka.producer.model.KTredjepartsvarsel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.concurrent.ListenableFuture;

import static java.time.LocalDateTime.now;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SoknadProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private TredjepartsvarselProducer tredjepartsvarselProducer;

    @Test
    public void sendOppgavevarsel() {
        when(kafkaTemplate.send(anyString(), anyString(), any(KTredjepartsvarsel.class))).thenReturn(mock(ListenableFuture.class));

        KTredjepartsvarsel kTredjepartsvarsel = KTredjepartsvarsel.builder()
                .nokkel(TredjepartsVarselNokkel.NAERMESTE_LEDER_SVAR_MOTEBEHOV.name())
                .ressursId("1")
                .aktorId("1010101010101")
                .epost("test@test.no")
                .mobilnr("99119911")
                .orgnummer("123456789")
                .utsendelsestidspunkt(now().plusMinutes(5))
                .build();

        tredjepartsvarselProducer.sendTredjepartsvarselvarsel(kTredjepartsvarsel);

        verify(kafkaTemplate).send(eq(tredjepartsvarselProducer.TREDJEPARTSVARSEL_TOPIC), anyString(), same(kTredjepartsvarsel));
    }
}
