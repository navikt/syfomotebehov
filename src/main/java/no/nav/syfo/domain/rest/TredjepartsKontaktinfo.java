package no.nav.syfo.domain.rest;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true, chain = true)
public class TredjepartsKontaktinfo {
    public String aktoerId;
    public String orgnummer;
    public String epost;
    public String mobil;
}
