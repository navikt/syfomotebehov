package no.nav.syfo.domain;

import lombok.*;

@Builder
@Value
@Getter
public class Dialogmotebehov{
    private String uuid;
    private String tidspunktFriskmelding;
    private String tiltak;
    private String resultatTiltak;
    private boolean trengerMote;
    private String behovDialogmote;
}
