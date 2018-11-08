package no.nav.syfo.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.spring.oidc.validation.api.ProtectedWithClaims;
import no.nav.syfo.domain.rest.Fnr;
import no.nav.syfo.domain.rest.Historikk;
import no.nav.syfo.domain.rest.Motebehov;
import no.nav.syfo.service.GeografiskTilgangService;
import no.nav.syfo.service.HistorikkService;
import no.nav.syfo.service.MotebehovService;
import no.nav.syfo.service.VeilederTilgangService;
import no.nav.syfo.util.Toggle;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolationException;
import javax.validation.constraints.Pattern;
import javax.ws.rs.ForbiddenException;
import java.io.IOException;
import java.util.List;

import static java.util.Collections.emptyList;
import static no.nav.syfo.OIDCIssuer.INTERN;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@RequestMapping(value = "/api/veileder")
public class MotebehovVeilederController {

    private HistorikkService historikkService;

    private MotebehovService motebehovService;

    private VeilederTilgangService veilederTilgangService;

    private GeografiskTilgangService geografiskTilgangService;

    @Inject
    public MotebehovVeilederController(
            final HistorikkService historikkService,
            final MotebehovService motebehovService,
            final VeilederTilgangService tilgangService,
            final GeografiskTilgangService geografiskTilgangService) {
        this.historikkService = historikkService;
        this.motebehovService = motebehovService;
        this.veilederTilgangService = tilgangService;
        this.geografiskTilgangService = geografiskTilgangService;
    }

    @ResponseBody
    @RequestMapping(value = "/motebehov")
    @ProtectedWithClaims(issuer = INTERN)
    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public List<Motebehov> hentMotebehovListe(@RequestParam(name = "fnr") @Pattern(regexp = "^[0-9]{11}$") String arbeidstakerFnr) {
        if (Toggle.endepunkterForMotebehov) {
            kastExceptionHvisIkkeTilgang(arbeidstakerFnr);

            if (!geografiskTilgangService.erBrukerTilhorendeMotebehovPilot(arbeidstakerFnr)) {
                return emptyList();
            }

            return motebehovService.hentMotebehovListe(Fnr.of(arbeidstakerFnr));
        } else {
            log.info("Det ble gjort kall mot 'veileder/motebehov', men dette endepunktet er togglet av.");
            return emptyList();
        }
    }

    @ResponseBody
    @RequestMapping(value = "/historikk")
    @ProtectedWithClaims(issuer = INTERN)
    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public List<Historikk> hentMotebehovHistorikk(@RequestParam(name = "fnr") @Pattern(regexp = "^[0-9]{11}$") String arbeidstakerFnr) {
        if (Toggle.endepunkterForMotebehov) {
            kastExceptionHvisIkkeTilgang(arbeidstakerFnr);

            if (!geografiskTilgangService.erBrukerTilhorendeMotebehovPilot(arbeidstakerFnr)) {
                return emptyList();
            }

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

    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
    void handleBadRequests(HttpServletResponse response) throws IOException {
        response.sendError(BAD_REQUEST.value(), "Vi kunne ikke tolke inndataene :/");
    }

    @ExceptionHandler({ForbiddenException.class})
    void handleForbiddenRequests(HttpServletResponse response) throws IOException {
        response.sendError(FORBIDDEN.value(), "Handling er forbudt");
    }

}
