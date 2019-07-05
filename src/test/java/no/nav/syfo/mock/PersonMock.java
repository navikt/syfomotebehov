package no.nav.syfo.mock;

import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "mockPerson_V3", havingValue = "true")
public class PersonMock implements PersonV3 {

    public final static String GEOGRAFISK_TILKNYTNING = "030109";
    public final static String PERSON_FORNAVN = "Fornavn";
    public final static String PERSON_ETTERNAVN = "Etternavn";
    public final static String PERSON_NAVN = PERSON_FORNAVN + " " + PERSON_ETTERNAVN;

    @Override
    public void ping() {
    }

    @Override
    public HentGeografiskTilknytningResponse hentGeografiskTilknytning(HentGeografiskTilknytningRequest hentGeografiskTilknytningRequest) {
        return new HentGeografiskTilknytningResponse()
                .withGeografiskTilknytning(new Kommune()
                        .withGeografiskTilknytning(GEOGRAFISK_TILKNYTNING));
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
                                .withFornavn(PERSON_FORNAVN)
                                .withEtternavn(PERSON_ETTERNAVN)
                        )
                        .withDiskresjonskode(new Diskresjonskoder()
                                .withValue(""))
                );
    }
}
