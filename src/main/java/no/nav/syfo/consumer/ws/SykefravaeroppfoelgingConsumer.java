package no.nav.syfo.consumer.ws;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.oidc.OIDCConstants;
import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.security.oidc.context.OIDCValidationContext;
import no.nav.syfo.OIDCIssuer;
import no.nav.syfo.config.ws.SykefravaersoppfoelgingConfig;
import no.nav.syfo.domain.rest.NaermesteLeder;
import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.HentNaermesteLederListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.HentNaermesteLedersAnsattListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.SykefravaersoppfoelgingV1;
import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.meldinger.WSHentNaermesteLederListeRequest;
import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.meldinger.WSHentNaermesteLederListeResponse;
import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.meldinger.WSHentNaermesteLedersAnsattListeRequest;
import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.meldinger.WSHentNaermesteLedersAnsattListeResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static no.nav.syfo.mappers.WSAnsattMapper.wsAnsatt2AktorId;
import static no.nav.syfo.mappers.WSNaermesteLederMapper.ws2naermesteLeder;
import static no.nav.syfo.util.MapUtil.mapListe;
import static no.nav.syfo.util.OIDCUtil.fnrFraOIDCEkstern;
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
    public SykefravaeroppfoelgingConsumer(final OIDCRequestContextHolder contextHolder,
                                          final SykefravaersoppfoelgingV1 sykefravaersoppfoelgingV1,
                                          final SykefravaersoppfoelgingConfig sykefravaersoppfoelgingConfig
    ) {
        this.contextHolder = contextHolder;
        this.sykefravaersoppfoelgingV1 = sykefravaersoppfoelgingV1;
        this.sykefravaersoppfoelgingConfig = sykefravaersoppfoelgingConfig;
    }

    public List<String> hentAnsatteAktorId(String aktoerId) {
        String oidcToken = tokenFraOIDC((OIDCValidationContext) this.contextHolder.getRequestAttribute(OIDCConstants.OIDC_VALIDATION_CONTEXT), OIDCIssuer.EKSTERN);

        try {
            WSHentNaermesteLedersAnsattListeRequest request = new WSHentNaermesteLedersAnsattListeRequest()
                    .withAktoerId(aktoerId);
            WSHentNaermesteLedersAnsattListeResponse response;
            if ("true".equals(dev)) {
                response = sykefravaersoppfoelgingV1.hentNaermesteLedersAnsattListe(request);
            } else {
                response = sykefravaersoppfoelgingConfig.hentNaermesteLedersAnsattListe(request, oidcToken);
            }
            return mapListe(response.getAnsattListe(), wsAnsatt2AktorId);
        } catch (HentNaermesteLedersAnsattListeSikkerhetsbegrensning e) {
            log.warn("{} fikk sikkerhetsbegrensning {} ved henting av ansatte for person {}", fnrFraOIDCEkstern(contextHolder), e.getFaultInfo().getFeilaarsak().toUpperCase(), aktoerId);
            throw new ForbiddenException();
        } catch (RuntimeException e) {
            log.error("{} fikk Runtimefeil ved henting av ansatte for person {}. " +
                    "Antar dette er tilgang nektet fra modig-security, og kaster ForbiddenException videre.", fnrFraOIDCEkstern(contextHolder), aktoerId, e);
            //TODO RuntimeException når SyfoService kaster sikkerhetsbegrensing riktig igjen
            throw new ForbiddenException();
        }
    }

    public List<NaermesteLeder> hentNaermesteLedere(String aktoerId) {
        String oidcToken = tokenFraOIDC((OIDCValidationContext) this.contextHolder.getRequestAttribute(OIDCConstants.OIDC_VALIDATION_CONTEXT), OIDCIssuer.EKSTERN);

        try {
            WSHentNaermesteLederListeRequest request = new WSHentNaermesteLederListeRequest()
                    .withAktoerId(aktoerId)
                    .withKunAktive(false);
            WSHentNaermesteLederListeResponse response;
            if ("true".equals(dev)) {
                response = sykefravaersoppfoelgingV1.hentNaermesteLederListe(request);
            } else {
                response = sykefravaersoppfoelgingConfig.hentNaermesteLederListe(request, oidcToken);
            }
            return mapListe(response.getNaermesteLederListe(), ws2naermesteLeder);
        } catch (HentNaermesteLederListeSikkerhetsbegrensning e) {
            log.warn("{} fikk sikkerhetsbegrensning {} ved henting av naermeste ledere for person {}", fnrFraOIDCEkstern(contextHolder), e.getFaultInfo().getFeilaarsak().toUpperCase(), aktoerId);
            throw new ForbiddenException();
        } catch (RuntimeException e) {
            log.error("{} fikk Runtimefeil ved henting av naermeste ledere for person {}" +
                    "Antar dette er tilgang nektet fra modig-security, og kaster ForbiddenException videre.", fnrFraOIDCEkstern(contextHolder), aktoerId, e);
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
