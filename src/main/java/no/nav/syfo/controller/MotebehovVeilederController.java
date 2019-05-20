package no.nav.syfo.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.spring.oidc.validation.api.ProtectedWithClaims;
import no.nav.syfo.domain.rest.*;
import no.nav.syfo.service.*;
import no.nav.syfo.util.Metrikk;
import no.nav.syfo.util.Toggle;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.validation.constraints.Pattern;
import javax.ws.rs.ForbiddenException;
import java.util.List;

import static java.util.Collections.emptyList;
import static no.nav.syfo.OIDCIssuer.INTERN;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@RequestMapping(value = "/api/veileder")
public class MotebehovVeilederController {

    private Metrikk metrikk;
    private HistorikkService historikkService;

    private MotebehovService motebehovService;

    private VeilederTilgangService veilederTilgangService;

    private GeografiskTilgangService geografiskTilgangService;

    @Inject
    public MotebehovVeilederController(
            final Metrikk metrikk,
            final HistorikkService historikkService,
            final MotebehovService motebehovService,
            final VeilederTilgangService tilgangService,
            final GeografiskTilgangService geografiskTilgangService) {
        this.metrikk = metrikk;
        this.historikkService = historikkService;
        this.motebehovService = motebehovService;
        this.veilederTilgangService = tilgangService;
        this.geografiskTilgangService = geografiskTilgangService;
    }

    @ProtectedWithClaims(issuer = INTERN)
    @GetMapping(value = "/motebehov", produces = APPLICATION_JSON_VALUE)
    public List<Motebehov> hentMotebehovListe(
            @RequestParam(name = "fnr") @Pattern(regexp = "^[0-9]{11}$") String arbeidstakerFnr
    ) {
        if (Toggle.endepunkterForMotebehov) {
            metrikk.tellEndepunktKall("veileder_hent_motebehov");

            kastExceptionHvisIkkeTilgang(arbeidstakerFnr);

            return motebehovService.hentMotebehovListe(Fnr.of(arbeidstakerFnr));
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

            return historikkService.hentHistorikkListe(Fnr.of(arbeidstakerFnr));
        } else {
            log.info("Det ble gjort kall mot 'veileder/historikk', men dette endepunktet er togglet av.");
            return emptyList();
        }
    }

    private void kastExceptionHvisIkkeTilgang(String fnr) {
        if (!veilederTilgangService.sjekkVeiledersTilgangTilPerson(fnr)) {
            throw new ForbiddenException("Veilederen har ikke tilgang til denne personen");
        }
    }
}
