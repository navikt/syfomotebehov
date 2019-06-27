package no.nav.syfo.domain.rest;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;

@Data
@Accessors(fluent = true, chain = true)
public class PeriodeDTO {
    public LocalDate fom;
    public LocalDate tom;
}
