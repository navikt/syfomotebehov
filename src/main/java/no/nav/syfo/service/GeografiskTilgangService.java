package no.nav.syfo.service;


import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.consumer.ws.OrganisasjonEnhetConsumer;
import no.nav.syfo.consumer.ws.PersonConsumer;
import no.nav.syfo.domain.rest.Fnr;
import no.nav.syfo.util.Toggle;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

@Slf4j
@Service
public class GeografiskTilgangService {

    private final PersonConsumer personConsumer;
    private final OrganisasjonEnhetConsumer organisasjonEnhetConsumer;

    @Inject
    public GeografiskTilgangService(final PersonConsumer personConsumer,
                                    final OrganisasjonEnhetConsumer organisasjonEnhetConsumer){
        this.personConsumer = personConsumer;
        this.organisasjonEnhetConsumer = organisasjonEnhetConsumer;
    }

    public List<String> hentBrukersNavKontorForGeografiskTilknytning(String fnr) {
        final String geografiskTilknytning = personConsumer.hentGeografiskTilknytning(fnr);
        if ("".equals(geografiskTilknytning)){
            return emptyList();
        }
        return organisasjonEnhetConsumer.finnNAVKontorForGT(geografiskTilknytning);
    }

    public List<String> hentPilotKontorer() {
        return ofNullable(Toggle.pilotKontorer)
                .map(kontorListe -> asList(kontorListe.split(",")))
                .orElse(emptyList());
    }

    public boolean erBrukerTilhorendeMotebehovPilot(String brukerFnr) {
        List<String> pilotKontorer = hentPilotKontorer();
        List<String> brukersNavkontorer = hentBrukersNavKontorForGeografiskTilknytning(brukerFnr);

        boolean erBrukerTilhorendeMotebehovPilot = brukersNavkontorer.stream().anyMatch(pilotKontorer::contains);

        // TODO: Fjern logging av typen MOTEBEHOV-TRACE etter verifisering av funksjonalitet i Prod.
        log.info("MOTEBEHOV-TRACE: Toggle {}, pilotkontorer {}, brukerkontorer {}, erBrukerTilhorendeMotebehovPilot {}",
                Toggle.endepunkterForMotebehov,
                Toggle.pilotKontorer,
                String.join(", ", brukersNavkontorer),
                erBrukerTilhorendeMotebehovPilot
        );

        return erBrukerTilhorendeMotebehovPilot;
    }
}
