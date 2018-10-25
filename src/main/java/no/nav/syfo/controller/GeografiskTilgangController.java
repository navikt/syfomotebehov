package no.nav.syfo.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.spring.oidc.validation.api.ProtectedWithClaims;
import no.nav.syfo.service.GeografiskTilgangService;
import no.nav.syfo.util.Toggle;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.validation.constraints.Pattern;
import javax.ws.rs.core.Response;

import static no.nav.syfo.OIDCIssuer.INTERN;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@RequestMapping(value = "/api/geografisktilgang")
public class GeografiskTilgangController {

    private GeografiskTilgangService geografiskTilgangService;

    @Inject
    public GeografiskTilgangController(
            final GeografiskTilgangService geografiskTilgangService) {
        this.geografiskTilgangService = geografiskTilgangService;
    }

    @ResponseBody
    @ProtectedWithClaims(issuer = INTERN, claimMap = {"sub=srvsyfoservice"})
    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public String hentGeografiskTilgang(@RequestParam(name = "fnr") @Pattern(regexp = "^[0-9]{11}$") String arbeidstakerFnr) {
        log.info("MOTEBEHOV-TRACE: Hent geografisk tilgang");
        if (Toggle.endepunkterForMotebehov) {
            if (geografiskTilgangService.erBrukerTilhorendeMotebehovPilot(arbeidstakerFnr)) {
                log.info("MOTEBEHOV-TRACE: Hent geografisk tilgang. Har tilgang");
                return "true";
            } else {
                log.info("MOTEBEHOV-TRACE: Hent geografisk tilgang. Ikke tilgang");
                return "false";
            }
        } else {
            log.info("Det ble gjort kall mot 'geografisktilgang', men dette endepunktet er togglet av.");
            return "false";
        }
    }
}