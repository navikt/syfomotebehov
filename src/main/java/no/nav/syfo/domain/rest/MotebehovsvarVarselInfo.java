package no.nav.syfo.domain.rest;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true, chain = true)
public class MotebehovsvarVarselInfo {
    public String sykmeldtAktorId;
    public String orgnummer;
}
