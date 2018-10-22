package no.nav.syfo.domain.rest;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(fluent = true, chain = true)
public class Historikk {
    public String opprettetAv;
    public String tekst;
    public LocalDateTime tidspunkt;
}
