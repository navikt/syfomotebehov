package no.nav.syfo.service;

import no.nav.syfo.consumer.ws.OrganisasjonEnhetConsumer;
import no.nav.syfo.consumer.ws.PersonConsumer;
import no.nav.syfo.util.Toggle;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

@Service
public class GeografiskTilgangService {

    private final PersonConsumer personConsumer;
    private final OrganisasjonEnhetConsumer organisasjonEnhetConsumer;

    @Inject
    public GeografiskTilgangService(
            PersonConsumer personConsumer,
            OrganisasjonEnhetConsumer organisasjonEnhetConsumer
    ) {
        this.personConsumer = personConsumer;
        this.organisasjonEnhetConsumer = organisasjonEnhetConsumer;
    }

    public List<String> hentBrukersNavKontorForGeografiskTilknytning(String fnr) {
        final String geografiskTilknytning = personConsumer.hentGeografiskTilknytning(fnr);
        if ("".equals(geografiskTilknytning)) {
            return emptyList();
        }
        return organisasjonEnhetConsumer.finnNAVKontorForGT(geografiskTilknytning);
    }

    public List<String> hentPilotKontorer() {
        return ofNullable(Toggle.pilotKontorer)
                .map(kontorListe -> asList(kontorListe.split(",")))
                .orElse(emptyList());
    }

    private boolean erBrukerTilhorendeMotebehovPilot(String brukerFnr) {
        List<String> pilotKontorer = hentPilotKontorer();
        List<String> brukersNavkontorer = hentBrukersNavKontorForGeografiskTilknytning(brukerFnr);

        return brukersNavkontorer.stream().anyMatch(pilotKontorer::contains);
    }

    public boolean erMotebehovTilgjengelig(String brukerFnr) {
        return Toggle.enableMotebehovNasjonal || erBrukerTilhorendeMotebehovPilot(brukerFnr);
    }
}
