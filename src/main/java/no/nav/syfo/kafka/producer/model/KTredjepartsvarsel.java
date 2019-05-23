package no.nav.syfo.kafka.producer.model;

import lombok.*;

import java.time.LocalDateTime;

@Value
@Builder
@Getter
public class KTredjepartsvarsel {
    private String type;
    private String ressursId;
    private String aktorId;
    private String orgnummer;
    private LocalDateTime utsendelsestidspunkt;
}
