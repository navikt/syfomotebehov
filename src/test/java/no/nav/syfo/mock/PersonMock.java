package no.nav.syfo.mock;

import no.nav.tjeneste.virksomhet.person.v3.*;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentSikkerhetstiltakResponse;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentEkteskapshistorikkResponse;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningResponse;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonerMedSammeAdresseResponse;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonhistorikkResponse;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonnavnBolkResponse;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentVergeResponse;
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.*;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "mockPerson_V3", havingValue = "true")
public class PersonMock implements PersonV3 {

    @Override
    public void ping() {
        return;
    }

    @Override
    public HentGeografiskTilknytningResponse hentGeografiskTilknytning(HentGeografiskTilknytningRequest hentGeografiskTilknytningRequest) {
        return new HentGeografiskTilknytningResponse()
                .withGeografiskTilknytning(new Kommune());
    }

    @Override
    public HentPersonhistorikkResponse hentPersonhistorikk(HentPersonhistorikkRequest hentPersonhistorikkRequest) {
        return null;
    }

    @Override
    public HentVergeResponse hentVerge(HentVergeRequest hentVergeRequest) {
        return null;
    }

    @Override
    public HentPersonerMedSammeAdresseResponse hentPersonerMedSammeAdresse(HentPersonerMedSammeAdresseRequest hentPersonerMedSammeAdresseRequest) {
        return null;
    }

    @Override
    public HentPersonnavnBolkResponse hentPersonnavnBolk(HentPersonnavnBolkRequest hentPersonnavnBolkRequest) {
        return null;
    }

    @Override
    public HentEkteskapshistorikkResponse hentEkteskapshistorikk(HentEkteskapshistorikkRequest hentEkteskapshistorikkRequest) {
        return null;
    }

    @Override
    public HentSikkerhetstiltakResponse hentSikkerhetstiltak(HentSikkerhetstiltakRequest hentSikkerhetstiltakRequest) {
        return null;
    }

    @Override
    public HentPersonResponse hentPerson(HentPersonRequest hentPersonRequest) {
        return new HentPersonResponse()
                .withPerson(new Person()
                    .withPersonnavn(new Personnavn()
                        .withFornavn("Sygve")
                        .withEtternavn("Sykmeldt")
                    )
                );
    }
}
