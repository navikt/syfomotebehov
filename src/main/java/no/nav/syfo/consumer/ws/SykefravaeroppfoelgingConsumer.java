package no.nav.syfo.consumer.ws;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.syfo.domain.rest.NaermesteLeder;
import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.HentNaermesteLederListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.HentNaermesteLedersAnsattListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.SykefravaersoppfoelgingV1;
import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.meldinger.WSHentNaermesteLederListeRequest;
import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.meldinger.WSHentNaermesteLederListeResponse;
import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.meldinger.WSHentNaermesteLedersAnsattListeRequest;
import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.meldinger.WSHentNaermesteLedersAnsattListeResponse;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static no.nav.syfo.mappers.WSAnsattMapper.wsAnsatt2AktorId;
import static no.nav.syfo.mappers.WSNaermesteLederMapper.ws2naermesteLeder;
import static no.nav.syfo.util.MapUtil.mapListe;
import static no.nav.syfo.util.OIDCUtil.fnrFraOIDC;

@Component
@Slf4j
public class SykefravaeroppfoelgingConsumer {

    private OIDCRequestContextHolder contextHolder;
    private SykefravaersoppfoelgingV1 sykefravaersoppfoelgingV1;

    @Inject
    public SykefravaeroppfoelgingConsumer(final OIDCRequestContextHolder contextHolder,
                                          final SykefravaersoppfoelgingV1 sykefravaersoppfoelgingV1
    ) {
        this.contextHolder = contextHolder;
        this.sykefravaersoppfoelgingV1 = sykefravaersoppfoelgingV1;
    }

    public List<String> hentAnsatteAktorId(String aktoerId) {
        log.error("Henter ansatte for aktoerId {}", aktoerId);
        try {
            WSHentNaermesteLedersAnsattListeResponse response = sykefravaersoppfoelgingV1
                    .hentNaermesteLedersAnsattListe(new WSHentNaermesteLedersAnsattListeRequest()
                            .withAktoerId(aktoerId));
            log.error("hentet ansatte 1", response.getAnsattListe().get(0).getAktoerId());
            log.error("hentet ansatte 2", mapListe(response.getAnsattListe(), wsAnsatt2AktorId).get(0));
            return mapListe(response.getAnsattListe(), wsAnsatt2AktorId);
        } catch (HentNaermesteLedersAnsattListeSikkerhetsbegrensning e) {
            log.warn("{} fikk sikkerhetsbegrensning {} ved henting av ansatte for person {}", fnrFraOIDC(contextHolder), e.getFaultInfo().getFeilaarsak().toUpperCase(), aktoerId);
            throw new ForbiddenException();
        } catch (RuntimeException e) {
            log.error("{} fikk Runtimefeil ved henting av ansatte for person {}. " +
                    "Antar dette er tilgang nektet fra modig-security, og kaster ForbiddenException videre.", fnrFraOIDC(contextHolder), aktoerId, e);
            //TODO RuntimeException når SyfoService kaster sikkerhetsbegrensing riktig igjen
            throw new ForbiddenException();
        }
    }

    public List<NaermesteLeder> hentNaermesteLedere(String aktoerId) {
        try {
            log.error("Henter ledere for aktoerId", aktoerId);
            WSHentNaermesteLederListeResponse response = sykefravaersoppfoelgingV1.hentNaermesteLederListe(new WSHentNaermesteLederListeRequest()
                    .withAktoerId(aktoerId)
                    .withKunAktive(false));
            return mapListe(response.getNaermesteLederListe(), ws2naermesteLeder);
        } catch (HentNaermesteLederListeSikkerhetsbegrensning e) {
            log.warn("{} fikk sikkerhetsbegrensning {} ved henting av naermeste ledere for person {}", fnrFraOIDC(contextHolder), e.getFaultInfo().getFeilaarsak().toUpperCase(), aktoerId);
            throw new ForbiddenException();
        } catch (RuntimeException e) {
            log.error("{} fikk Runtimefeil ved henting av naermeste ledere for person {}" +
                    "Antar dette er tilgang nektet fra modig-security, og kaster ForbiddenException videre.", fnrFraOIDC(contextHolder), aktoerId, e);
            //TODO RuntimeException når SyfoService kaster sikkerhetsbegrensing riktig igjen
            throw new ForbiddenException();
        }
    }

    public List<String> hentNaermesteLederAktoerIdListe(String aktoerId) {
        return hentNaermesteLedere(aktoerId).stream()
                .map(ansatt -> ansatt.naermesteLederAktoerId)
                .collect(toList());
    }
}
