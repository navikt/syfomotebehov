package no.nav.syfo.domain.rest;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(fluent = true, chain = true)
@EqualsAndHashCode
public class Motebehov {
    public String id;
    public LocalDateTime opprettetDato;
    public String opprettetAv;
    public Person arbeidstaker;
    public String virksomhetsnummer;
    public MotebehovSvar motebehovSvar;
}
