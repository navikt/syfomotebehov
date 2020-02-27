package no.nav.syfo.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.security.spring.oidc.validation.api.ProtectedWithClaims;
import no.nav.syfo.aktorregister.domain.Fodselsnummer;
import no.nav.syfo.domain.rest.Fnr;
import no.nav.syfo.domain.rest.Motebehov;
import no.nav.syfo.historikk.Historikk;
import no.nav.syfo.historikk.HistorikkService;
import no.nav.syfo.service.MotebehovService;
import no.nav.syfo.service.VeilederTilgangService;
import no.nav.syfo.util.Metrikk;
import no.nav.syfo.util.Toggle;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.validation.constraints.Pattern;
import javax.ws.rs.ForbiddenException;
import java.util.List;

import static java.util.Collections.emptyList;
import static no.nav.syfo.oidc.OIDCIssuer.INTERN;
import static no.nav.syfo.util.OIDCUtil.getSubjectFromOIDCToken;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@RequestMapping(value = "/api/veileder")
public class MotebehovVeilederController {

    private final OIDCRequestContextHolder oidcCtxHolder;

    private final Metrikk metrikk;

    private final HistorikkService historikkService;

    private final MotebehovService motebehovService;

    private final VeilederTilgangService veilederTilgangService;

    @Inject
    public MotebehovVeilederController(
            OIDCRequestContextHolder oidcCtxHolder,
            Metrikk metrikk,
            HistorikkService historikkService,
            MotebehovService motebehovService,
            VeilederTilgangService tilgangService
    ) {
        this.oidcCtxHolder = oidcCtxHolder;
        this.metrikk = metrikk;
        this.historikkService = historikkService;
        this.motebehovService = motebehovService;
        this.veilederTilgangService = tilgangService;
    }

    @ProtectedWithClaims(issuer = INTERN)
    @GetMapping(value = "/motebehov", produces = APPLICATION_JSON_VALUE)
    public List<Motebehov> hentMotebehovListe(
            @RequestParam(name = "fnr") @Pattern(regexp = "^[0-9]{11}$") String arbeidstakerFnr
    ) {
        if (Toggle.endepunkterForMotebehov) {
            metrikk.tellEndepunktKall("veileder_hent_motebehov");

            kastExceptionHvisIkkeTilgang(arbeidstakerFnr);

            return motebehovService.hentMotebehovListe(new Fodselsnummer(arbeidstakerFnr));
        } else {
            log.info("Det ble gjort kall mot 'veileder/motebehov', men dette endepunktet er togglet av.");
            return emptyList();
        }
    }

    @ProtectedWithClaims(issuer = INTERN)
    @GetMapping(value = "/historikk", produces = APPLICATION_JSON_VALUE)
    public List<Historikk> hentMotebehovHistorikk(
            @RequestParam(name = "fnr") @Pattern(regexp = "^[0-9]{11}$") String arbeidstakerFnr
    ) {
        if (Toggle.endepunkterForMotebehov) {
            metrikk.tellEndepunktKall("veileder_hent_motebehov_historikk");

            kastExceptionHvisIkkeTilgang(arbeidstakerFnr);

            return historikkService.hentHistorikkListe(arbeidstakerFnr);
        } else {
            log.info("Det ble gjort kall mot 'veileder/historikk', men dette endepunktet er togglet av.");
            return emptyList();
        }
    }

    @ProtectedWithClaims(issuer = INTERN)
    @PostMapping(value = "/motebehov/{fnr}/behandle")
    public void behandleMotebehov(
            @PathVariable(name = "fnr") @Pattern(regexp = "^[0-9]{11}$") String arbeidstakerFnr
    ) {
        if (Toggle.endepunkterForMotebehov) {
            metrikk.tellEndepunktKall("veileder_behandle_motebehov_call");

            kastExceptionHvisIkkeTilgang(arbeidstakerFnr);

            motebehovService.behandleUbehandledeMotebehov(Fnr.of(arbeidstakerFnr), getSubjectFromOIDCToken(oidcCtxHolder, INTERN));

            metrikk.tellEndepunktKall("veileder_behandle_motebehov_success");
        } else {
            log.info("Det ble gjort kall mot 'veileder/motebehov/behandle', men dette endepunktet er togglet av.");
        }
    }

    private void kastExceptionHvisIkkeTilgang(String fnr) {
        if (!veilederTilgangService.sjekkVeiledersTilgangTilPerson(fnr)) {
            throw new ForbiddenException("Veilederen har ikke tilgang til denne personen");
        }
    }
}
