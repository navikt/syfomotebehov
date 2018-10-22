package no.nav.syfo.domain.rest;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true, chain = true)
public class MotebehovSvar {
    public boolean harMotebehov;
    public String friskmeldingForventning;
    public String tiltak;
    public String tiltakResultat;
    public String forklaring;
}
