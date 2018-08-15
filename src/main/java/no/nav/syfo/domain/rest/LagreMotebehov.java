package no.nav.syfo.domain.rest;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
@Accessors(fluent = true, chain = true)
@EqualsAndHashCode
public class LagreMotebehov {

    @Valid
    public Fnr arbeidstakerFnr;

    @NotEmpty
    public String virksomhetsnummer;

    @NotNull
    public MotebehovSvar motebehovSvar;
}
