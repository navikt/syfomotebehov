package no.nav.syfo.consumer.ws;

import lombok.extern.slf4j.Slf4j;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.AktoerId;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personnavn;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonRequest;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.text.WordUtils.capitalize;

@Component
@Slf4j
@CacheConfig(cacheNames = "person")
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
        log.info("JTRACE: hentNavnFraAktoerId med aktoerId {}", aktoerId);
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
            log.error("Fikk RuntimeException mot TPS med ved oppslag av aktoerId: " + aktoerId);
            return "";
        }
    }

    public boolean erBrukerKode6(String aktoerId) {
        return "6".equals(hentDiskresjonskodeForAktoer(aktoerId));
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
            return person.getDiskresjonskode().getValue();
        } catch (HentPersonSikkerhetsbegrensning e) {
            log.error("Fikk sikkerhetsbegrensing ved oppslag med aktoerId: " + aktoerId);
            throw new ForbiddenException();
        } catch (HentPersonPersonIkkeFunnet e) {
            log.error("Fant ikke person med aktoerId: " + aktoerId);
            throw new RuntimeException();
        } catch (RuntimeException e) {
            log.error("Fikk RuntimeException mot TPS med ved oppslag av aktoerId: " + aktoerId);
            return "";
        }
    }

}
