package no.nav.syfo.domain.rest;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;

@Data
@Accessors(fluent = true, chain = true)
public class Oppfolgingstilfelle {
    public String orgnummer;
    public LocalDate fom;
    public LocalDate tom;
    public int grad;
    public String aktivitet;
}
