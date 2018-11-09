package no.nav.syfo.mappers.domain;


import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
public class Enhet {
    String enhetId;
    String navn;
}
