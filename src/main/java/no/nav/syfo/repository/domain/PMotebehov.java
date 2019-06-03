package no.nav.syfo.repository.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Accessors(fluent = true, chain = true)
public class PMotebehov {
    public UUID uuid;
    public LocalDateTime opprettetDato;
    public String opprettetAv;
    public String aktoerId;
    public String virksomhetsnummer;
    public String friskmeldingForventning;
    public String tiltak;
    public String tiltakResultat;
    public boolean harMotebehov;
    public String forklaring;
    public String tildeltEnhet;
}
