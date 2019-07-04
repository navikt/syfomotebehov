package no.nav.syfo.kafka;

import no.nav.syfo.kafka.producer.OversikthendelseProducer;
import no.nav.syfo.kafka.producer.model.KOversikthendelse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.concurrent.ListenableFuture;

import static java.time.LocalDateTime.now;
import static no.nav.syfo.kafka.producer.OversikthendelseProducer.OVERSIKTHENDELSE_TOPIC;
import static no.nav.syfo.kafka.producer.OversikthendelseType.MOTEBEHOV_SVAR_BEHANDLET;
import static no.nav.syfo.kafka.producer.OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT;
import static no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR;
import static no.nav.syfo.testhelper.UserConstants.NAV_ENHET;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OversikthendelseProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private OversikthendelseProducer oversikthendelseProducer;

    @Test
    public void sendOversikthendelse() {
        when(kafkaTemplate.send(anyString(), anyString(), any(KOversikthendelse.class))).thenReturn(mock(ListenableFuture.class));

        KOversikthendelse kOversikthendelse = KOversikthendelse.builder()
                .fnr(ARBEIDSTAKER_FNR)
                .hendelseId(MOTEBEHOV_SVAR_MOTTATT.name())
                .enhetId(NAV_ENHET)
                .tidspunkt(now())
                .build();

        oversikthendelseProducer.sendOversikthendelse(kOversikthendelse);

        verify(kafkaTemplate).send(eq(OVERSIKTHENDELSE_TOPIC), anyString(), same(kOversikthendelse));
    }

    @Test
    public void sendOversikthendelseMotebehovSvarBehandlet() {
        when(kafkaTemplate.send(anyString(), anyString(), any(KOversikthendelse.class))).thenReturn(mock(ListenableFuture.class));

        KOversikthendelse kOversikthendelse = KOversikthendelse.builder()
                .fnr(ARBEIDSTAKER_FNR)
                .hendelseId(MOTEBEHOV_SVAR_BEHANDLET.name())
                .enhetId(NAV_ENHET)
                .tidspunkt(now())
                .build();

        oversikthendelseProducer.sendOversikthendelse(kOversikthendelse);

        verify(kafkaTemplate).send(eq(OVERSIKTHENDELSE_TOPIC), anyString(), same(kOversikthendelse));
    }
}
