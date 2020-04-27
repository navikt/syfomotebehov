package no.nav.syfo.kafka.producer;

import no.nav.syfo.kafka.producer.model.KOversikthendelse;
import org.slf4j.Logger;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import static java.util.UUID.randomUUID;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class OversikthendelseProducer {

    private static final Logger log = getLogger(OversikthendelseProducer.class);

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
            log.info("Legger oversikthendelse med id {} på kø for enhet {}", kOversikthendelse.hendelseId(), kOversikthendelse.enhetId());
        } catch (Exception e) {
            log.error("Feil ved sending av oppgavevarsel", e);
            throw new RuntimeException("Feil ved sending av oppgavevarsel", e);
        }
    }
}
