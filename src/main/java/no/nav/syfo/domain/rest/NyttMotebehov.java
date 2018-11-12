package no.nav.syfo.domain.rest;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
@Accessors(fluent = true, chain = true)
public class NyttMotebehov {

    public String arbeidstakerFnr;

    @NotEmpty
    public String virksomhetsnummer;

    @NotNull
    public MotebehovSvar motebehovSvar;

    public String tildeltEnhet;
}
