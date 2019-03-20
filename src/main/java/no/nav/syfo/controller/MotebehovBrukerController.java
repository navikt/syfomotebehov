package no.nav.syfo.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.security.spring.oidc.validation.api.ProtectedWithClaims;
import no.nav.syfo.domain.rest.*;
import no.nav.syfo.service.*;
import no.nav.syfo.util.Metrikk;
import no.nav.syfo.util.Toggle;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.ws.rs.ForbiddenException;
import java.util.List;

import static java.util.Collections.emptyList;
import static no.nav.syfo.OIDCIssuer.EKSTERN;
import static no.nav.syfo.util.OIDCUtil.fnrFraOIDCEkstern;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@ProtectedWithClaims(issuer = EKSTERN, claimMap = {"acr=Level4"})
@RequestMapping(value = "/api/motebehov")
public class MotebehovBrukerController {

    private OIDCRequestContextHolder contextHolder;
    private Metrikk metrikk;
    private MotebehovService motebehovService;
    private BrukertilgangService brukertilgangService;
    private GeografiskTilgangService geografiskTilgangService;

    @Inject
    public MotebehovBrukerController(
            final OIDCRequestContextHolder contextHolder,
            final Metrikk metrikk,
            final MotebehovService motebehovService,
            final BrukertilgangService brukertilgangService,
            final GeografiskTilgangService geografiskTilgangService
    ) {
        this.contextHolder = contextHolder;
        this.metrikk = metrikk;
        this.motebehovService = motebehovService;
        this.brukertilgangService = brukertilgangService;
        this.geografiskTilgangService = geografiskTilgangService;
    }

    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public List<Motebehov> hentMotebehovListe(
            @RequestParam(name = "fnr") @Pattern(regexp = "^[0-9]{11}$") String arbeidstakerFnr,
            @RequestParam(name = "virksomhetsnummer") String virksomhetsnummer
    ) {
        if (Toggle.endepunkterForMotebehov) {
            Fnr fnr = StringUtils.isEmpty(arbeidstakerFnr) ? fnrFraOIDCEkstern(contextHolder) : Fnr.of(arbeidstakerFnr);

            kastExceptionHvisIkkeTilgang(fnr.getFnr());

            if (!virksomhetsnummer.isEmpty()) {
                return motebehovService.hentMotebehovListeForArbeidstakerOpprettetAvLeder(fnr, virksomhetsnummer);
            }
            return motebehovService.hentMotebehovListeForOgOpprettetAvArbeidstaker(fnr);
        } else {
            log.info("Det ble gjort kall mot 'motebehov', men dette endepunktet er togglet av.");
            return emptyList();
        }
    }

    @PostMapping(consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public void lagreMotebehov(
            @RequestBody @Valid NyttMotebehov nyttMotebehov
    ) {
        if (Toggle.endepunkterForMotebehov) {
            Fnr fnr = StringUtils.isEmpty(nyttMotebehov.arbeidstakerFnr) ? fnrFraOIDCEkstern(contextHolder) : Fnr.of(nyttMotebehov.arbeidstakerFnr);

            kastExceptionHvisIkkeTilgang(fnr.getFnr());

            motebehovService.lagreMotebehov(fnrFraOIDCEkstern(contextHolder), fnr, nyttMotebehov);
            lagBesvarMotebehovMetrikk(nyttMotebehov);

        } else {
            log.info("Det ble gjort kall mot 'motebehov', men dette endepunktet er togglet av.");
        }
    }

    private void kastExceptionHvisIkkeTilgang(String fnr) {
        String innloggetIdent = fnrFraOIDCEkstern(contextHolder).getFnr();
        boolean harTilgang = geografiskTilgangService.erMotebehovTilgjengelig(fnr) && brukertilgangService.harTilgangTilOppslaattBruker(innloggetIdent, fnr);
        if (!harTilgang) {
            throw new ForbiddenException("Ikke tilgang");
        }
    }

    private void lagBesvarMotebehovMetrikk(NyttMotebehov nyttMotebehov) {
        boolean erInnloggetBrukerArbeidstaker = StringUtils.isEmpty(nyttMotebehov.arbeidstakerFnr);
        MotebehovSvar motebehovSvar = nyttMotebehov.motebehovSvar;
        metrikk.tellMotebehovBesvart(motebehovSvar.harMotebehov, erInnloggetBrukerArbeidstaker);

        if (!motebehovSvar.harMotebehov) {
            metrikk.tellMotebehovBesvartNeiAntallTegn(motebehovSvar.forklaring.length(), erInnloggetBrukerArbeidstaker);
        } else if (!StringUtils.isEmpty(motebehovSvar.forklaring)) {
            metrikk.tellMotebehovBesvartJaMedForklaringTegn(motebehovSvar.forklaring.length(), erInnloggetBrukerArbeidstaker);
            metrikk.tellMotebehovBesvartJaMedForklaringAntall(erInnloggetBrukerArbeidstaker);
        }
    }
}
