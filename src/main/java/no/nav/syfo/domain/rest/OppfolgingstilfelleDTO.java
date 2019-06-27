package no.nav.syfo.domain.rest;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true, chain = true)
public class OppfolgingstilfelleDTO {
    public int antallBrukteDager;
    public boolean oppbruktArbeidsgvierperiode;
    public PeriodeDTO arbeidsgiverperiode;
}
