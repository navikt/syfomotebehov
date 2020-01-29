package no.nav.syfo.consumer.ws;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.syfo.config.ws.SykefravaersoppfoelgingConfig;
import no.nav.syfo.domain.rest.Oppfolgingstilfelle;
import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.HentSykeforlopperiodeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.SykefravaersoppfoelgingV1;
import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.informasjon.WSSykeforlopperiode;
import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.meldinger.WSHentSykeforlopperiodeRequest;
import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.meldinger.WSHentSykeforlopperiodeResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;
import java.util.List;

import static java.util.stream.Collectors.toList;
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
