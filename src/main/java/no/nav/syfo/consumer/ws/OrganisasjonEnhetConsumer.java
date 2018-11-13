package no.nav.syfo.consumer.ws;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.mappers.domain.Enhet;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.FinnNAVKontorUgyldigInput;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.HentOverordnetEnhetListeEnhetIkkeFunnet;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.OrganisasjonEnhetV2;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.informasjon.WSEnhetRelasjonstyper;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.informasjon.WSEnhetsstatus;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.informasjon.WSGeografi;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.informasjon.WSOrganisasjonsenhet;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.meldinger.WSFinnNAVKontorRequest;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.meldinger.WSFinnNAVKontorResponse;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.meldinger.WSHentOverordnetEnhetListeRequest;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

import static no.nav.tjeneste.virksomhet.organisasjonenhet.v2.informasjon.WSEnhetsstatus.AKTIV;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;

@Component
@Slf4j
public class OrganisasjonEnhetConsumer implements InitializingBean {

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

    @Cacheable("orgnkontorgeografisk")
    public List<String> finnNAVKontorForGT(String geografiskTilknytning) {
        try {
            return of(organisasjonEnhetV2.finnNAVKontor(
                    new WSFinnNAVKontorRequest()
                            .withGeografiskTilknytning(
                                    new WSGeografi()
                                            .withValue(geografiskTilknytning))))
                    .map(WSFinnNAVKontorResponse::getNAVKontor)
                    .filter(wsOrganisasjonsenhet -> AKTIV.equals(wsOrganisasjonsenhet.getStatus()))
                    .map(WSOrganisasjonsenhet::getEnhetId)
                    .collect(toList());
        } catch (FinnNAVKontorUgyldigInput |
                RuntimeException e) {
            log.info("Finner ikke NAV-kontor for geografisk tilknytning " + geografiskTilknytning, e);
            return emptyList();
        }
    }

    public Optional<Enhet> finnSetteKontor(String enhet) {
        try {
            return organisasjonEnhetV2.hentOverordnetEnhetListe(new WSHentOverordnetEnhetListeRequest()
                    .withEnhetId(enhet).withEnhetRelasjonstype(new WSEnhetRelasjonstyper().withValue("HABILITET")))
                    .getOverordnetEnhetListe()
                    .stream()
                    .filter(wsOrganisasjonsenhet -> AKTIV.equals(wsOrganisasjonsenhet.getStatus()))
                    .map(wsOrganisasjonsenhet -> new Enhet().enhetId(wsOrganisasjonsenhet.getEnhetId()).navn(wsOrganisasjonsenhet.getEnhetNavn()))
                    .findFirst();
        } catch (HentOverordnetEnhetListeEnhetIkkeFunnet e) {
            log.error("Fant ingen overordnet enhet");
            throw new RuntimeException();
        }
    }

}
