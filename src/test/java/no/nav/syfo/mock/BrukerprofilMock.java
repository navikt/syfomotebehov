package no.nav.syfo.mock;

import no.nav.tjeneste.virksomhet.brukerprofil.v3.*;
import no.nav.tjeneste.virksomhet.brukerprofil.v3.informasjon.WSBruker;
import no.nav.tjeneste.virksomhet.brukerprofil.v3.informasjon.WSNorskIdent;
import no.nav.tjeneste.virksomhet.brukerprofil.v3.informasjon.WSPersonnavn;
import no.nav.tjeneste.virksomhet.brukerprofil.v3.meldinger.WSHentKontaktinformasjonOgPreferanserRequest;
import no.nav.tjeneste.virksomhet.brukerprofil.v3.meldinger.WSHentKontaktinformasjonOgPreferanserResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "mockBrukerprofil_V3", havingValue = "true")
public class BrukerprofilMock implements BrukerprofilV3 {

    @Override
    public void ping() {
        return;
    }

    @Override
    public WSHentKontaktinformasjonOgPreferanserResponse hentKontaktinformasjonOgPreferanser(WSHentKontaktinformasjonOgPreferanserRequest wsHentKontaktinformasjonOgPreferanserRequest) throws HentKontaktinformasjonOgPreferanserSikkerhetsbegrensning, HentKontaktinformasjonOgPreferanserPersonIkkeFunnet, HentKontaktinformasjonOgPreferanserPersonIdentErUtgaatt {
        return new WSHentKontaktinformasjonOgPreferanserResponse()
                .withBruker(new WSBruker()
                        .withIdent(new WSNorskIdent()
                                .withIdent("03097043123"))
                        .withPersonnavn(new WSPersonnavn()
                                .withFornavn("Sygve")
                                .withEtternavn("Sykmeldt")));
    }
}
