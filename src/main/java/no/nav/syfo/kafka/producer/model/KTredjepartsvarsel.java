package no.nav.syfo.kafka.producer.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
@Getter
public class KTredjepartsvarsel {
    private String type;
    private String ressursId;
    private String aktorId;
    private String epost;
    private String mobilnr;
    private String orgnummer;
    private LocalDateTime utsendelsestidspunkt;
}
