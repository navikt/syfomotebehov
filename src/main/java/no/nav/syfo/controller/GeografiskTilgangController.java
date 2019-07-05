package no.nav.syfo.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.spring.oidc.validation.api.ProtectedWithClaims;
import no.nav.syfo.util.Toggle;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.Response;

import static no.nav.syfo.OIDCIssuer.INTERN;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@RequestMapping(value = "/api/geografisktilgang")
public class GeografiskTilgangController {

    @Inject
    public GeografiskTilgangController() {
    }

    @ProtectedWithClaims(issuer = INTERN, claimMap = {"sub=srvsyfoservice"})
    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public Response hentGeografiskTilgang() {
        if (Toggle.endepunkterForMotebehov) {
            return Response.ok().build();
        } else {
            log.info("Det ble gjort kall mot 'geografisktilgang', men dette endepunktet er togglet av.");
            throw new ForbiddenException("Ikke tilgang");
        }
    }
}
