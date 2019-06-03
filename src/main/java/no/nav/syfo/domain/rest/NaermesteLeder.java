package no.nav.syfo.domain.rest;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
public class NaermesteLeder {
    public long naermesteLederId;
    public String naermesteLederAktoerId;
    public String orgnummer;
    public NaermesteLederStatus naermesteLederStatus;
    public String navn;
    public String mobil;
    public String epost;
}
