package no.nav.syfo.domain.rest;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.time.Month;


@Data
@Accessors(fluent = true)
@EqualsAndHashCode
public class VeilederOppgave {

    public long id;
    public String type;
    public String tildeltIdent;
    public String tildeltEnhet;
    public String lenke;
    public String fnr;
    public String virksomhetsnummer;
    public String created;
    public String sistEndret;
    public String sistEndretAv;
    public String status;
    public String uuid;

    public LocalDateTime getCreated() {
        int aar = Integer.parseInt(this.created.substring(0, 4));
        Month month = Month.of(Integer.parseInt(this.created.substring(6, 7)));
        int day = Integer.parseInt(this.created.substring(8, 10));
        return LocalDateTime.of(aar, month, day, 0, 0);
    }

    public LocalDateTime getSistEndret() {
        int aar = Integer.parseInt(this.sistEndret.substring(0, 4));
        Month month = Month.of(Integer.parseInt(this.sistEndret.substring(6, 7)));
        int day = Integer.parseInt(this.sistEndret.substring(8, 10));
        return LocalDateTime.of(aar, month, day, 0, 0);
    }
}
