package no.nav.syfo.mock;

import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Diskresjonskoder;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Kommune;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personnavn;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.*;
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
