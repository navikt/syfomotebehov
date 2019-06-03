package no.nav.syfo.domain.rest;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;

@Data
@Accessors(fluent = true)
public class NaermesteLederStatus {
    public boolean erAktiv;
    public LocalDate aktivFom;
    public LocalDate aktivTom;
}
