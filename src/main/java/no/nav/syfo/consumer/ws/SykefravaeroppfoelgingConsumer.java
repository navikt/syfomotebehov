package no.nav.syfo.consumer.ws;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.syfo.config.ws.SykefravaersoppfoelgingConfig;
import no.nav.syfo.domain.rest.NaermesteLeder;
import no.nav.syfo.domain.rest.Oppfolgingstilfelle;
import no.nav.syfo.mappers.domain.Hendelse;
import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.*;
import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.informasjon.WSSykeforlopperiode;
import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.meldinger.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static no.nav.syfo.mappers.WSAnsattMapper.wsAnsatt2AktorId;
import static no.nav.syfo.mappers.WSHendelseMapper.ws2Hendelse;
import static no.nav.syfo.mappers.WSNaermesteLederMapper.ws2naermesteLeder;
import static no.nav.syfo.util.MapUtil.mapListe;
import static no.nav.syfo.util.OIDCUtil.tokenFraOIDC;

@Component
@Slf4j
public class SykefravaeroppfoelgingConsumer {

    @Value("${dev}")
    private String dev;
    private OIDCRequestContextHolder contextHolder;
    private SykefravaersoppfoelgingV1 sykefravaersoppfoelgingV1;
    private SykefravaersoppfoelgingConfig sykefravaersoppfoelgingConfig;

    @Inject
    public SykefravaeroppfoelgingConsumer(
            final OIDCRequestContextHolder contextHolder,
            final SykefravaersoppfoelgingV1 sykefravaersoppfoelgingV1,
            final SykefravaersoppfoelgingConfig sykefravaersoppfoelgingConfig
    ) {
        this.contextHolder = contextHolder;
        this.sykefravaersoppfoelgingV1 = sykefravaersoppfoelgingV1;
        this.sykefravaersoppfoelgingConfig = sykefravaersoppfoelgingConfig;
    }

    public List<String> hentAnsatteAktorId(String aktoerId, String oidcIssuer) {
        try {
            WSHentNaermesteLedersAnsattListeRequest request = new WSHentNaermesteLedersAnsattListeRequest()
                    .withAktoerId(aktoerId);
            WSHentNaermesteLedersAnsattListeResponse response;
            if ("true".equals(dev)) {
                response = sykefravaersoppfoelgingV1.hentNaermesteLedersAnsattListe(request);
            } else {
                String oidcToken = tokenFraOIDC(this.contextHolder, oidcIssuer);
                response = sykefravaersoppfoelgingConfig.hentNaermesteLedersAnsattListe(request, oidcToken);
            }
            return mapListe(response.getAnsattListe(), wsAnsatt2AktorId);
        } catch (HentNaermesteLedersAnsattListeSikkerhetsbegrensning e) {
            log.warn("Fikk sikkerhetsbegrensning {} ved henting av ansatte for person {}", e.getFaultInfo().getFeilaarsak().toUpperCase(), aktoerId);
            throw new ForbiddenException();
        } catch (RuntimeException e) {
            log.error("Fikk Runtimefeil ved henting av ansatte for person {}. " +
                    "Antar dette er tilgang nektet fra modig-security, og kaster ForbiddenException videre.", aktoerId, e);
            //TODO RuntimeException når SyfoService kaster sikkerhetsbegrensing riktig igjen
            throw new ForbiddenException();
        }
    }

    public List<NaermesteLeder> hentNaermesteLedere(String aktoerId, String oidcIssuer) {
        try {
            WSHentNaermesteLederListeRequest request = new WSHentNaermesteLederListeRequest()
                    .withAktoerId(aktoerId)
                    .withKunAktive(false);
            WSHentNaermesteLederListeResponse response;
            if ("true".equals(dev)) {
                response = sykefravaersoppfoelgingV1.hentNaermesteLederListe(request);
            } else {
                String oidcToken = tokenFraOIDC(this.contextHolder, oidcIssuer);
                response = sykefravaersoppfoelgingConfig.hentNaermesteLederListe(request, oidcToken);
            }
            return mapListe(response.getNaermesteLederListe(), ws2naermesteLeder);
        } catch (HentNaermesteLederListeSikkerhetsbegrensning e) {
            log.warn("Fikk sikkerhetsbegrensning {} ved henting av naermeste ledere for person {}", e.getFaultInfo().getFeilaarsak().toUpperCase(), aktoerId);
            throw new ForbiddenException();
        } catch (RuntimeException e) {
            log.error("Fikk Runtimefeil ved henting av naermeste ledere for person {}" +
                    "Antar dette er tilgang nektet fra modig-security, og kaster ForbiddenException videre.", aktoerId, e);
            //TODO RuntimeException når SyfoService kaster sikkerhetsbegrensing riktig igjen
            throw new ForbiddenException();
        }
    }

    public List<String> hentNaermesteLederAktoerIdListe(String aktoerId, String oidcIssuer) {
        return hentNaermesteLedere(aktoerId, oidcIssuer).stream()
                .map(ansatt -> ansatt.naermesteLederAktoerId)
                .collect(toList());
    }

    public List<Hendelse> hentHendelserForSykmeldt(String aktoerId, String oidcIssuer) {
        try {
            WSHentHendelseListeRequest request = new WSHentHendelseListeRequest()
                    .withAktoerId(aktoerId);
            WSHentHendelseListeResponse response;
            if ("true".equals(dev)) {
                response = sykefravaersoppfoelgingV1.hentHendelseListe(request);
            } else {
                String oidcToken = tokenFraOIDC(this.contextHolder, oidcIssuer);
                response = sykefravaersoppfoelgingConfig.hentHendelseListe(request, oidcToken);
            }
            return mapListe(response.getHendelseListe(), ws2Hendelse);
        } catch (HentHendelseListeSikkerhetsbegrensning e) {
            log.warn("Fikk sikkerhetsbegrensning {} ved henting av hendelseliste for person {}", e.getFaultInfo().getFeilaarsak().toUpperCase(), aktoerId);
            throw new ForbiddenException();
        } catch (RuntimeException e) {
            log.error("Fikk Runtimefeil ved henting av naermeste ledere for person {}" +
                    "Antar dette er tilgang nektet fra modig-security, og kaster ForbiddenException videre.", aktoerId, e);
            //TODO RuntimeException når SyfoService kaster sikkerhetsbegrensing riktig igjen
            throw new ForbiddenException();
        }
    }

    public List<Oppfolgingstilfelle> hentOppfolgingstilfelleperioder(String aktorId, String orgnummer, String oidcIssuer) {
        try {
            log.info("L-TRACE: Forsøker å hente oppfolgingstilfelleperioder fra syfoservice med aktorId: {}, orgnummer: {}, oidcIssuer: {}", aktorId, orgnummer, oidcIssuer);
            WSHentSykeforlopperiodeRequest request = new WSHentSykeforlopperiodeRequest()
                    .withAktoerId(aktorId)
                    .withOrgnummer(orgnummer);
            WSHentSykeforlopperiodeResponse response;

            if ("true".equals(dev)) {
                log.info("L-TRACE: Er i dev!");
                response = sykefravaersoppfoelgingV1.hentSykeforlopperiode(request);
                log.info("L-TRACE: Fikk response fra dev: {}", response);
            } else {
                log.info("L-TRACE: Er i prod!");
                String oidcToken = tokenFraOIDC(this.contextHolder, oidcIssuer);
                response = sykefravaersoppfoelgingConfig.hentSykeforlopperiode(request, oidcToken);
                log.info("L-TRACE: Fikk response: {}", response);
            }
            return tilSykeforlopperiodeListe(response.getSykeforlopperiodeListe(), orgnummer);
        } catch (HentSykeforlopperiodeSikkerhetsbegrensning e) {
            log.info("L-TRACE: Fikk sikkerhetsbegrensning fra syfoservice", e);
            log.warn("Sikkerhetsbegrensning ved henting av oppfølgingstilfelleperioder");
            throw new ForbiddenException();
        } catch (RuntimeException e) {
            log.info("L-TRACE: Fikk runtimefeil fra syfoservice", e);
            log.error("Fikk Runtimefeil ved henting av sykeforlopperioder" +
                    "Antar dette er tilgang nektet fra modig-security, og kaster ForbiddenException videre.", e);
            //TODO RuntimeException når SyfoService kaster sikkerhetsbegrensing riktig igjen
            throw new ForbiddenException();
        }
    }

    private List<Oppfolgingstilfelle> tilSykeforlopperiodeListe(List<WSSykeforlopperiode> wsSykeforlopperiodeListe, String orgnummer) {
        log.info("L-TRACE: Map til sykeforlop {}", wsSykeforlopperiodeListe);
        return wsSykeforlopperiodeListe
                .stream()
                .map(wsSykeforlopperiode -> new Oppfolgingstilfelle()
                        .orgnummer(orgnummer)
                        .fom(wsSykeforlopperiode.getFom())
                        .tom(wsSykeforlopperiode.getTom())
                        .grad(wsSykeforlopperiode.getGrad())
                        .aktivitet(wsSykeforlopperiode.getAktivitet()))
                .collect(toList());
    }
}
