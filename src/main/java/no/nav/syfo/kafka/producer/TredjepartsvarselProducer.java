package no.nav.syfo.kafka.producer;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.kafka.producer.model.KTredjepartsvarsel;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import static java.util.UUID.randomUUID;

@Component
@Slf4j
public class TredjepartsvarselProducer {
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
            log.info("Legger tredjepartsvarsel med ressursID {} på kø for aktor {}", kTredjepartsvarsel.getRessursId(), kTredjepartsvarsel.getAktorId());
        } catch (Exception e) {
            log.error("Feil ved sending av oppgavevarsel", e);
            throw new RuntimeException("Feil ved sending av oppgavevarsel", e);
        }
    }
}
