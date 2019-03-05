package no.nav.syfo.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.spring.oidc.validation.api.ProtectedWithClaims;
import no.nav.syfo.service.GeografiskTilgangService;
import no.nav.syfo.util.Toggle;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.Pattern;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static no.nav.syfo.OIDCIssuer.INTERN;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@RequestMapping(value = "/api/geografisktilgang")
public class GeografiskTilgangController {

    private GeografiskTilgangService geografiskTilgangService;

    @Inject
    public GeografiskTilgangController(
            final GeografiskTilgangService geografiskTilgangService
    ) {
        this.geografiskTilgangService = geografiskTilgangService;
    }

    @ResponseBody
    @ProtectedWithClaims(issuer = INTERN, claimMap = {"sub=srvsyfoservice"})
    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public Response hentGeografiskTilgang(
            @RequestParam(name = "fnr") @Pattern(regexp = "^[0-9]{11}$") String arbeidstakerFnr
    ) {
        if (Toggle.endepunkterForMotebehov) {
            if (geografiskTilgangService.erMotebehovTilgjengelig(arbeidstakerFnr)) {
                return Response.ok().build();
            } else {
                throw new ForbiddenException("Ikke tilgang");
            }
        } else {
            log.info("Det ble gjort kall mot 'geografisktilgang', men dette endepunktet er togglet av.");
            throw new ForbiddenException("Ikke tilgang");
        }
    }

    @ExceptionHandler({ForbiddenException.class})
    void handleForbiddenRequests(HttpServletResponse response) throws IOException {
        response.sendError(FORBIDDEN.value(), "Handling er forbudt");
    }
}
