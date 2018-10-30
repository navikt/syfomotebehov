package no.nav.syfo.mappers.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(fluent = true)
public class Brukeroppgave {
    public String oppgaveUUID;
    public String oppgavetype;
    public String ident;
    public String ressursId;
    public String ressursEier;
    LocalDateTime opprettetTidspunkt;
}
