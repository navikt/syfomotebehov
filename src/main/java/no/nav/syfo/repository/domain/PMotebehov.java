package no.nav.syfo.repository.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import java.time.LocalDateTime;

@Builder
@Value
@Getter
public class PMotebehov {
    private String uuid;
    public LocalDateTime opprettetDato;
    private String opprettetAv;
    private String aktoerId;
    private String virksomhetsnummer;
    private String friskmeldingForventning;
    private String tiltak;
    private String tiltakResultat;
    private boolean harMotebehov;
    private String forklaring;
}
