package no.nav.syfo.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import java.time.LocalDateTime;

@Builder
@Value
@Getter
public class Dialogmotebehov {
    private String id;
    private LocalDateTime opprettetDato;
    private String opprettetAv;
    private String aktoerId;
    private String friskmeldingForventning;
    private String tiltak;
    private String tiltakResultat;
    private boolean harMotebehov;
    private String forklaring;
}
