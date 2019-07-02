package no.nav.syfo.kafka.producer;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.kafka.producer.model.KOversikthendelse;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import static java.util.UUID.randomUUID;

@Component
@Slf4j
public class OversikthendelseProducer {
    private KafkaTemplate<String, Object> kafkaTemplate;

    public static final String OVERSIKTHENDELSE_TOPIC = "aapen-syfo-oversikthendelse-v1";

    public OversikthendelseProducer(
            KafkaTemplate<String, Object> kafkaTemplate
    ) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendOversikthendelse(KOversikthendelse kOversikthendelse) {
        try {
            kafkaTemplate.send(
                    OVERSIKTHENDELSE_TOPIC,
                    randomUUID().toString(),
                    kOversikthendelse
            ).get();
            log.info("Legger oversikthendelse med id {} på kø for enhet {}", kOversikthendelse.getHendelseId(), kOversikthendelse.getEnhetId());
        } catch (Exception e) {
            log.error("Feil ved sending av oppgavevarsel", e);
            throw new RuntimeException("Feil ved sending av oppgavevarsel", e);
        }
    }
}
