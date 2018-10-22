package no.nav.syfo.consumer.ws;

import lombok.extern.slf4j.Slf4j;
import no.nav.tjeneste.virksomhet.organisasjon.v4.HentOrganisasjonOrganisasjonIkkeFunnet;
import no.nav.tjeneste.virksomhet.organisasjon.v4.HentOrganisasjonUgyldigInput;
import no.nav.tjeneste.virksomhet.organisasjon.v4.OrganisasjonV4;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.WSOrganisasjon;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.WSOrganisasjonsDetaljer;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.WSOrganisasjonsnavn;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.WSUstrukturertNavn;
import no.nav.tjeneste.virksomhet.organisasjon.v4.meldinger.WSHentOrganisasjonRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.LocalDate;
import java.util.Collection;
import java.util.function.Predicate;

import static java.util.Optional.of;
import static java.util.stream.Collectors.joining;

@Component
@Slf4j
public class OrganisasjonConsumer implements InitializingBean {

    private static OrganisasjonConsumer instance;

    private final OrganisasjonV4 organisasjonV4;

    @Override
    public void afterPropertiesSet() { instance = this; }

    public static OrganisasjonConsumer organisasjonConsumer() { return instance; }

    @Inject
    public OrganisasjonConsumer(OrganisasjonV4 organisasjonV4) { this.organisasjonV4 = organisasjonV4; }


    public String hentBedriftnavn(String orgnr) {

        try {
            WSOrganisasjon organisasjon = organisasjonV4.hentOrganisasjon(new WSHentOrganisasjonRequest()
                    .withInkluderHistorikk(false)
                    .withInkluderHierarki(false)
                    .withOrgnummer(orgnr)
            ).getOrganisasjon();
            return hentBedriftNavnFraWS(organisasjon, LocalDate.now(), orgnr);
        } catch (HentOrganisasjonOrganisasjonIkkeFunnet | HentOrganisasjonUgyldigInput e) {
            log.warn("Kunne ikke hente organisasjon for {}", orgnr, e);
            throw new RuntimeException();
        } catch (RuntimeException e) {
            log.error("Feil ved henting av Organisasjon", e);
            throw new RuntimeException();
        }
    }

    private String hentBedriftNavnFraWS(WSOrganisasjon organisasjon, LocalDate dato, String orgnr) {
        return of(organisasjon)
                .map(WSOrganisasjon::getOrganisasjonDetaljer)
                .map(WSOrganisasjonsDetaljer::getNavn)
                .map(Collection::stream)
                .flatMap(navn -> navn
                        .filter(finnesIPeriode(dato))
                        .map(WSOrganisasjonsnavn::getNavn)
                        .map(wsSammensattNavn -> (WSUstrukturertNavn) wsSammensattNavn)
                        .map(wsUstrukturertNavn -> wsUstrukturertNavn.getNavnelinje().stream()
                                .filter(StringUtils::isNotBlank)
                                .collect(joining(", ")))
                        .findFirst())
                .orElseThrow(() -> {
                    log.warn("Kunne ikke hente organisasjon for {}", orgnr);
                    return new RuntimeException();
        });
    }

    private Predicate<WSOrganisasjonsnavn> finnesIPeriode(LocalDate dato) {
        return navn -> !dato.isBefore(navn.getFomGyldighetsperiode().toGregorianCalendar().toZonedDateTime().toLocalDate()) &&
                (navn.getTomGyldighetsperiode() == null || !dato.isAfter(navn.getTomGyldighetsperiode().toGregorianCalendar().toZonedDateTime().toLocalDate()));
    }

}
