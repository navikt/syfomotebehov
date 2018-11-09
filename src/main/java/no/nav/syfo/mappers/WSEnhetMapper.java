package no.nav.syfo.mappers;

import no.nav.syfo.mappers.domain.Enhet;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.WSOrganisasjonsenhet;
import java.util.function.Function;

public class WSEnhetMapper {
    public static Function<WSOrganisasjonsenhet, Enhet> ws2Enhet = wsEnhet -> new Enhet()
            .enhetId(wsEnhet.getEnhetId())
            .navn(wsEnhet.getEnhetNavn());
}
