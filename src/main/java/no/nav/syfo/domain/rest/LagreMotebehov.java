package no.nav.syfo.domain.rest;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true, chain = true)
@EqualsAndHashCode
public class LagreMotebehov {
    public String arbeidstakerFnr;
    public MotebehovSvar motebehovSvar;
}
