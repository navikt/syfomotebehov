package no.nav.syfo.consumer.ws;

import lombok.extern.slf4j.Slf4j;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.FinnNAVKontorUgyldigInput;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.OrganisasjonEnhetV2;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.informasjon.WSEnhetsstatus;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.informasjon.WSGeografi;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.informasjon.WSOrganisasjonsenhet;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.meldinger.WSFinnNAVKontorRequest;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.meldinger.WSFinnNAVKontorResponse;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;

@Component
@Slf4j
public class OrganisasjonEnhetConsumer  implements InitializingBean {

    private static OrganisasjonEnhetConsumer instance;

    private final OrganisasjonEnhetV2 organisasjonEnhetV2;

    @Override
    public void afterPropertiesSet() {
        instance = this;
    }

    public static OrganisasjonEnhetConsumer organisasjonEnhetConsumer() {
        return instance;
    }

    @Inject
    public OrganisasjonEnhetConsumer(OrganisasjonEnhetV2 organisasjonEnhetV2) {
        this.organisasjonEnhetV2 = organisasjonEnhetV2;
    }

    public List<String> finnNAVKontorForGT(String geografiskTilknytning) {
        if("030109".equals(geografiskTilknytning)){
            return asList("0330");
        } else{
            return asList("0314");
        }
        /*try {
            return of(organisasjonEnhetV2.finnNAVKontor(
                    new WSFinnNAVKontorRequest()
                            .withGeografiskTilknytning(
                                    new WSGeografi()
                                            .withValue(geografiskTilknytning))))
                    .map(WSFinnNAVKontorResponse::getNAVKontor)
                    .filter(wsOrganisasjonsenhet -> WSEnhetsstatus.AKTIV.equals(wsOrganisasjonsenhet.getStatus()))
                    .map(WSOrganisasjonsenhet::getEnhetId)
                    .collect(toList());
        } catch (FinnNAVKontorUgyldigInput |
                RuntimeException e) {
            log.info("Finner ikke NAV-kontor for geografisk tilknytning " + geografiskTilknytning, e);
            return emptyList();
        }*/
    }

}
