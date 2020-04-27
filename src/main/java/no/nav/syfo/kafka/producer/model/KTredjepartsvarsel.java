package no.nav.syfo.kafka.producer.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(fluent = true, chain = true)
public class KTredjepartsvarsel {
    public String type;
    public String ressursId;
    public String aktorId;
    public String orgnummer;
    public LocalDateTime utsendelsestidspunkt;
}
