package no.nav.syfo.domain.rest;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(fluent = true)
public class VeilederOppgaveFeedItem {
    public String uuid;
    public String type;
    public String tildeltIdent;
    public String tildeltEnhet;
    public String lenke;
    public String fnr;
    public String virksomhetsnummer;
    public LocalDateTime created;
    public LocalDateTime sistEndret;
    public String sistEndretAv;
    public String status;


    public enum FeedHendelseType {
        MOTEBEHOV_MOTTATT,
    }
}
