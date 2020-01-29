package no.nav.syfo.consumer.ws;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.syfo.config.ws.SykefravaersoppfoelgingConfig;
import no.nav.syfo.domain.rest.Oppfolgingstilfelle;
import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.*;
import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.informasjon.WSSykeforlopperiode;
import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.meldinger.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.remoting.soap.SoapFaultException;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.InternalServerErrorException;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static no.nav.syfo.mappers.WSAnsattMapper.wsAnsatt2AktorId;
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
        } catch (SoapFaultException e) {
            log.warn("Fikk Soap feil ved henting av ansatte for person {}. " +
                    "Antar dette er tilgang nektet fra modig-security, og kaster ForbiddenException videre.", aktoerId, e);
            //TODO Fjern denne når SyfoService kaster sikkerhetsbegrensing riktig igjen
            throw new ForbiddenException();
        } catch (RuntimeException e) {
            log.error("Fikk runtime feil ved henting av ansatte for person {}. " +
                    "Kaster ForbiddenException videre.", aktoerId, e);
            throw new InternalServerErrorException();
        }
    }

    public List<Oppfolgingstilfelle> hentOppfolgingstilfelleperioder(String aktorId, String orgnummer, String oidcIssuer) {
        try {
            WSHentSykeforlopperiodeRequest request = new WSHentSykeforlopperiodeRequest()
                    .withAktoerId(aktorId)
                    .withOrgnummer(orgnummer);
            WSHentSykeforlopperiodeResponse response;

            if ("true".equals(dev)) {
                response = sykefravaersoppfoelgingV1.hentSykeforlopperiode(request);
            } else {
                String oidcToken = tokenFraOIDC(this.contextHolder, oidcIssuer);
                response = sykefravaersoppfoelgingConfig.hentSykeforlopperiode(request, oidcToken);
            }
            return tilSykeforlopperiodeListe(response.getSykeforlopperiodeListe(), orgnummer);
        } catch (HentSykeforlopperiodeSikkerhetsbegrensning e) {
            log.warn("Sikkerhetsbegrensning ved henting av oppfølgingstilfelleperioder");
            throw new ForbiddenException();
        } catch (RuntimeException e) {
            log.error("Fikk Runtimefeil ved henting av sykeforlopperioder" +
                    "Antar dette er tilgang nektet fra modig-security, og kaster ForbiddenException videre.", e);
            //TODO RuntimeException når SyfoService kaster sikkerhetsbegrensing riktig igjen
            throw new ForbiddenException();
        }
    }

    private List<Oppfolgingstilfelle> tilSykeforlopperiodeListe(List<WSSykeforlopperiode> wsSykeforlopperiodeListe, String orgnummer) {
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
