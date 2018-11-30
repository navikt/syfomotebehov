package no.nav.syfo.domain.rest;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
public class BrukerPaaEnhet {
    public String fnr;
    public boolean skjermetEllerEgenAnsatt;
}
