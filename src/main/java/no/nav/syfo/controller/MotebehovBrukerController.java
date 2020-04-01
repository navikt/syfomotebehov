package no.nav.syfo.controller;

import no.nav.security.oidc.api.ProtectedWithClaims;
import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.syfo.aktorregister.domain.Fodselsnummer;
import no.nav.syfo.brukertilgang.BrukertilgangService;
import no.nav.syfo.domain.rest.*;
import no.nav.syfo.service.MotebehovService;
import no.nav.syfo.util.Metrikk;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.ws.rs.ForbiddenException;
import java.util.List;

import static no.nav.syfo.oidc.OIDCIssuer.EKSTERN;
import static no.nav.syfo.util.OIDCUtil.fnrFraOIDCEkstern;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@ProtectedWithClaims(issuer = EKSTERN, claimMap = {"acr=Level4"})
@RequestMapping(value = "/api/motebehov")
public class MotebehovBrukerController {

    private OIDCRequestContextHolder contextHolder;
    private Metrikk metrikk;
    private MotebehovService motebehovService;
    private BrukertilgangService brukertilgangService;

    @Inject
    public MotebehovBrukerController(
            final OIDCRequestContextHolder contextHolder,
            final Metrikk metrikk,
            final MotebehovService motebehovService,
            final BrukertilgangService brukertilgangService
    ) {
        this.contextHolder = contextHolder;
        this.metrikk = metrikk;
        this.motebehovService = motebehovService;
        this.brukertilgangService = brukertilgangService;
    }

    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public List<Motebehov> hentMotebehovListe(
            @RequestParam(name = "fnr") @Pattern(regexp = "^[0-9]{11}$") String arbeidstakerFnr,
            @RequestParam(name = "virksomhetsnummer") String virksomhetsnummer
    ) {
        Fodselsnummer fnr = StringUtils.isEmpty(arbeidstakerFnr) ? fnrFraOIDCEkstern(contextHolder) : new Fodselsnummer(arbeidstakerFnr);

        kastExceptionHvisIkkeTilgang(fnr.getValue());

        if (!virksomhetsnummer.isEmpty()) {
            return motebehovService.hentMotebehovListeForArbeidstakerOpprettetAvLeder(fnr, virksomhetsnummer);
        }
        return motebehovService.hentMotebehovListeForOgOpprettetAvArbeidstaker(fnr);
    }

    @PostMapping(consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public void lagreMotebehov(
            @RequestBody @Valid NyttMotebehov nyttMotebehov
    ) {
        Fodselsnummer fnr = StringUtils.isEmpty(nyttMotebehov.arbeidstakerFnr) ? fnrFraOIDCEkstern(contextHolder) : new Fodselsnummer(nyttMotebehov.arbeidstakerFnr);

        kastExceptionHvisIkkeTilgang(fnr.getValue());

        boolean erInnloggetBrukerArbeidstaker = StringUtils.isEmpty(nyttMotebehov.arbeidstakerFnr);

        motebehovService.lagreMotebehov(fnrFraOIDCEkstern(contextHolder), fnr, nyttMotebehov);

        lagBesvarMotebehovMetrikk(nyttMotebehov, erInnloggetBrukerArbeidstaker);
    }

    private void kastExceptionHvisIkkeTilgang(String fnr) {
        String innloggetIdent = fnrFraOIDCEkstern(contextHolder).getValue();
        boolean harTilgang = brukertilgangService.harTilgangTilOppslaattBruker(innloggetIdent, fnr);
        if (!harTilgang) {
            throw new ForbiddenException("Ikke tilgang");
        }
    }

    private void lagBesvarMotebehovMetrikk(NyttMotebehov nyttMotebehov, boolean erInnloggetBrukerArbeidstaker) {
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
