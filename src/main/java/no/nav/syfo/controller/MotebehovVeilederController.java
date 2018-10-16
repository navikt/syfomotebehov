package no.nav.syfo.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.spring.oidc.validation.api.ProtectedWithClaims;
import no.nav.syfo.domain.rest.Fnr;
import no.nav.syfo.domain.rest.Historikk;
import no.nav.syfo.domain.rest.Motebehov;
import no.nav.syfo.service.GeografiskTilgangService;
import no.nav.syfo.service.HistorikkService;
import no.nav.syfo.service.MotebehovService;
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
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@RequestMapping(value = "/api/veileder")
public class MotebehovVeilederController {

    private HistorikkService historikkService;

    private MotebehovService motebehovService;

    private GeografiskTilgangService geografiskTilgangService;

    @Inject
    public MotebehovVeilederController(
            final HistorikkService historikkService,
            final MotebehovService motebehovService,
            final GeografiskTilgangService geografiskTilgangService) {
        this.historikkService = historikkService;
        this.motebehovService = motebehovService;
        this.geografiskTilgangService = geografiskTilgangService;
    }

    @ResponseBody
    @RequestMapping(value = "/motebehov")
    @ProtectedWithClaims(issuer = INTERN)
    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public List<Motebehov> hentMotebehovListe(@RequestParam(name = "fnr") @Pattern(regexp = "^[0-9]{11}$") String arbeidstakerFnr) {
        if (Toggle.endepunkterForMotebehov) {
            if(!geografiskTilgangService.erBrukerTilhorendeMotebehovPilot(arbeidstakerFnr)){
                log.info("Det ble gjort kall mot 'veileder/motebehov' men sykmeldt er ikke i et av pilotkontorene");
                throw new ForbiddenException("Sykmeldt hører ikke til et av pilotkontorene");
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
            if(!geografiskTilgangService.erBrukerTilhorendeMotebehovPilot(arbeidstakerFnr)){
                log.info("Det ble gjort kall mot 'veileder/historikk' men bruker er ikke i et av pilotkontorene");
                throw new ForbiddenException("Sykmeldt hører ikke til et av pilotkontorene");
            }
            return historikkService.hentHistorikkListe(Fnr.of(arbeidstakerFnr));
        } else {
            log.info("Det ble gjort kall mot 'veileder/historikk', men dette endepunktet er togglet av.");
            return emptyList();
        }
    }

    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
    void handleBadRequests(HttpServletResponse response) throws IOException {
        response.sendError(BAD_REQUEST.value(), "Vi kunne ikke tolke inndataene :/");
    }

}
