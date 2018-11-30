package no.nav.syfo.consumer.ws;

import lombok.extern.slf4j.Slf4j;
import no.nav.tjeneste.virksomhet.person.v3.binding.*;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningRequest;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonRequest;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.text.WordUtils.capitalize;

@Component
@Slf4j
public class PersonConsumer implements InitializingBean {

    private static PersonConsumer instance;

    private final PersonV3 personV3;

    @Override
    public void afterPropertiesSet() {
        instance = this;
    }

    public static PersonConsumer personConsumer() {
        return instance;
    }

    @Inject
    public PersonConsumer(PersonV3 personV3) {
        this.personV3 = personV3;
    }

    @Cacheable("personnavn")
    public String hentNavnFraAktoerId(String aktoerId) {
        if (isBlank(aktoerId) || !aktoerId.matches("\\d{13}$")) {
            log.error("Ugyldig format på aktoerId: " + aktoerId);
            throw new IllegalArgumentException();
        }
        try {
            Person person = personV3.hentPerson(new HentPersonRequest()
                    .withAktoer(new AktoerId()
                            .withAktoerId(aktoerId)))
                    .getPerson();
            Personnavn navnFraTps = person.getPersonnavn();
            String mellomnavn = navnFraTps.getMellomnavn();
            mellomnavn = mellomnavn != null ? mellomnavn + " " : "";
            final String personNavn = navnFraTps.getFornavn() + " " + mellomnavn + navnFraTps.getEtternavn();
            return capitalize(personNavn.toLowerCase(), '-', ' ');
        } catch (HentPersonSikkerhetsbegrensning e) {
            log.error("Fikk sikkerhetsbegrensing ved oppslag med aktoerId: " + aktoerId);
            throw new ForbiddenException();
        } catch (HentPersonPersonIkkeFunnet e) {
            log.error("Fant ikke person med aktoerId: " + aktoerId);
            throw new RuntimeException();
        } catch (RuntimeException e) {
            log.error("Fikk RuntimeException mot TPS for navn ved oppslag av aktoerId: " + aktoerId);
            return "";
        }
    }

    public boolean erBrukerKode6(String aktoerId) {
        return "6".equals(hentDiskresjonskodeForAktoer(aktoerId));
    }

    public boolean erBrukerDiskresjonsmerket(String aktoerId) {
        String diskresjonskode = hentDiskresjonskodeForAktoer(aktoerId);
        return "6".equals(diskresjonskode) || "7".equals(diskresjonskode);
    }

    @Cacheable("persondiskresjonskode")
    public String hentDiskresjonskodeForAktoer(String aktoerId) {
        if (isBlank(aktoerId) || !aktoerId.matches("\\d{13}$")) {
            log.error("Ugyldig format på aktoerId: " + aktoerId);
            throw new IllegalArgumentException();
        }
        try {
            Person person = personV3.hentPerson(new HentPersonRequest()
                    .withAktoer(new AktoerId()
                            .withAktoerId(aktoerId)))
                    .getPerson();
            return ofNullable(person.getDiskresjonskode()).map(Diskresjonskoder::getValue).orElse("");
        } catch (HentPersonSikkerhetsbegrensning e) {
            log.error("Fikk sikkerhetsbegrensing ved oppslag med aktoerId: " + aktoerId);
            throw new ForbiddenException();
        } catch (HentPersonPersonIkkeFunnet e) {
            log.error("Fant ikke person med aktoerId: " + aktoerId);
            throw new RuntimeException();
        } catch (RuntimeException e) {
            log.error("Fikk RuntimeException mot TPS for diskresjonskode ved oppslag av aktoerId: " + aktoerId);
            return "";
        }
    }

    @Cacheable("persongeografisk")
    public String hentGeografiskTilknytning(String fnr) {
        try {
            GeografiskTilknytning geografiskTilknytning = personV3.hentGeografiskTilknytning(
                    new HentGeografiskTilknytningRequest()
                            .withAktoer(new PersonIdent().withIdent(new NorskIdent().withIdent(fnr))))
                    .getGeografiskTilknytning();
            return ofNullable(geografiskTilknytning).map(GeografiskTilknytning::getGeografiskTilknytning).orElse("");
        } catch (HentGeografiskTilknytningSikkerhetsbegrensing e) {
            log.error("Fikk sikkerhetsbegrensing ved henting av geografiskTilknytning");
            throw new ForbiddenException();
        } catch (HentGeografiskTilknytningPersonIkkeFunnet e) {
            log.error("Fant ikke person ved henting av geografiskTilknytning");
            throw new RuntimeException();
        } catch (RuntimeException e) {
            log.error("Fikk RuntimeException mote TPS for diskresjonskode ved henting av geografiskTilknytning");
            return "";
        }
    }

}
