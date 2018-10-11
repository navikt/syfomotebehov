package no.nav.syfo.domain.rest;


import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
@EqualsAndHashCode
public class Tilgang {
    public boolean harTilgang;
    public String begrunnelse;
}
