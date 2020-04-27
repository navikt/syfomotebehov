package no.nav.syfo.kafka.producer;

import no.nav.syfo.kafka.producer.model.KTredjepartsvarsel;
import org.slf4j.Logger;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import static java.util.UUID.randomUUID;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class TredjepartsvarselProducer {

    private static final Logger log = getLogger(TredjepartsvarselProducer.class);

    private KafkaTemplate<String, Object> kafkaTemplate;

    public final String TREDJEPARTSVARSEL_TOPIC = "aapen-syfo-tredjepartsvarsel-v1";

    public TredjepartsvarselProducer(
            KafkaTemplate<String, Object> kafkaTemplate
    ) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendTredjepartsvarselvarsel(KTredjepartsvarsel kTredjepartsvarsel) {
        try {
            kafkaTemplate.send(
                    TREDJEPARTSVARSEL_TOPIC,
                    randomUUID().toString(),
                    kTredjepartsvarsel).get();
            log.info("Legger tredjepartsvarsel med ressursID {} på kø for aktor {}", kTredjepartsvarsel.ressursId(), kTredjepartsvarsel.aktorId());
        } catch (Exception e) {
            log.error("Feil ved sending av oppgavevarsel", e);
            throw new RuntimeException("Feil ved sending av oppgavevarsel", e);
        }
    }
}
