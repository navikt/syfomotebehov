package no.nav.syfo.domain.rest;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Accessors(fluent = true, chain = true)
public class Motebehov {
    public UUID id;
    public LocalDateTime opprettetDato;
    public String aktorId;
    public String opprettetAv;
    public String arbeidstakerFnr;
    public String virksomhetsnummer;
    public MotebehovSvar motebehovSvar;
    public String tildeltEnhet;
    public LocalDateTime behandletTidspunkt;
    public String behandletVeilederIdent;
}
