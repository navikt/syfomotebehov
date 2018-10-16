package no.nav.syfo.mock;

import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.OrganisasjonEnhetV2;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.informasjon.WSEnhetsstatus;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.informasjon.WSOrganisasjonsenhet;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.meldinger.*;
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personnavn;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "mockOrganisasjonEnhet_V2", havingValue = "true")
public class OrganisasjonEnhetMock implements OrganisasjonEnhetV2 {

    @Override
    public void ping() {
        return;
    }

    @Override
    public WSFinnNAVKontorResponse finnNAVKontor(WSFinnNAVKontorRequest wsFinnNAVKontorRequest){
        return new WSFinnNAVKontorResponse()
                .withNAVKontor(new WSOrganisasjonsenhet()
                .withEnhetId("0330")
                .withEnhetNavn("Bjerke")
                .withStatus(WSEnhetsstatus.AKTIV)
                );
    }

    @Override
    public WSHentFullstendigEnhetListeResponse hentFullstendigEnhetListe(WSHentFullstendigEnhetListeRequest wsHentFullstendigEnhetListeRequest){
        return null;
    }

    @Override
    public WSHentEnhetBolkResponse hentEnhetBolk(WSHentEnhetBolkRequest wsHentEnhetBolkRequest){
        return null;
    }

    @Override
    public WSHentOverordnetEnhetListeResponse hentOverordnetEnhetListe(WSHentOverordnetEnhetListeRequest wsHentOverordnetEnhetListeRequest){
        return null;
    }


}
