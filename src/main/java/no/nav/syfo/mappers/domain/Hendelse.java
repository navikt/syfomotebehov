package no.nav.syfo.mappers.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(fluent = true)
public class Hendelse {
    public long hendelseId;
    public LocalDateTime tidspunkt;
    public String type;
    public String aktorId;
}
