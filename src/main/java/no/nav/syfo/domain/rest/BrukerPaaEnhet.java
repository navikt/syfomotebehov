package no.nav.syfo.domain.rest;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
public class BrukerPaaEnhet {
    public String fnr;
    public Skjermingskode skjermetEllerEgenAnsatt;

    public enum Skjermingskode {
        KODE_6,
        KODE_7,
        EGEN_ANSATT,
        INGEN
    }
}

