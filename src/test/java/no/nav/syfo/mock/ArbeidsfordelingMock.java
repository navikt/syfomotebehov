package no.nav.syfo.mock;

import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.ArbeidsfordelingV1;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.WSEnhetsstatus;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.WSOrganisasjonsenhet;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.meldinger.WSFinnAlleBehandlendeEnheterListeRequest;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.meldinger.WSFinnAlleBehandlendeEnheterListeResponse;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.meldinger.WSFinnBehandlendeEnhetListeRequest;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.meldinger.WSFinnBehandlendeEnhetListeResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import static java.util.Arrays.asList;

@Service
@ConditionalOnProperty(value = "mockArbeidsfordeling_V1", havingValue = "true")
public class ArbeidsfordelingMock implements ArbeidsfordelingV1 {

    @Override
    public void ping() { return; }

    @Override
    public WSFinnAlleBehandlendeEnheterListeResponse
    finnAlleBehandlendeEnheterListe(WSFinnAlleBehandlendeEnheterListeRequest request) {
        return new WSFinnAlleBehandlendeEnheterListeResponse().withBehandlendeEnhetListe(asList(
                new WSOrganisasjonsenhet()
                        .withEnhetId("0330")
                        .withEnhetNavn("NAV Bjerke")
                        .withStatus(
                                WSEnhetsstatus.fromValue("AKTIV")
                        ),
                new WSOrganisasjonsenhet()
                        .withEnhetNavn("0314")
                        .withEnhetNavn("NAV Sagene")
                        .withStatus(
                                WSEnhetsstatus.fromValue("AKTIV")
                        )
        ));
    }

    @Override
    public WSFinnBehandlendeEnhetListeResponse
    finnBehandlendeEnhetListe(WSFinnBehandlendeEnhetListeRequest request) {
        return new WSFinnBehandlendeEnhetListeResponse().withBehandlendeEnhetListe(asList(
                new WSOrganisasjonsenhet()
                        .withEnhetId("0330")
                        .withEnhetNavn("NAV Bjerke")
                        .withStatus(
                                WSEnhetsstatus.fromValue("AKTIV")
                        ),
                new WSOrganisasjonsenhet()
                        .withEnhetNavn("0314")
                        .withEnhetNavn("NAV Sagene")
                        .withStatus(
                                WSEnhetsstatus.fromValue("AKTIV")
                        )
        ));
    }
}
