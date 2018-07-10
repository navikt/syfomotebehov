package no.nav.syfo.repository.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import java.time.LocalDateTime;

@Builder
@Value
@Getter
public class PDialogmotebehov {
    private String uuid;
    private LocalDateTime opprettetDato;
    private String opprettetAv;
    private String aktoerId;
    private String friskmeldingForventning;
    private String tiltak;
    private String tiltakResultat;
    private boolean harMotebehov;
    private String forklaring;
}
