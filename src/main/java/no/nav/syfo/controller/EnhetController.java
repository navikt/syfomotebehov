package no.nav.syfo.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.spring.oidc.validation.api.ProtectedWithClaims;
import no.nav.syfo.domain.rest.BrukerPaaEnhet;
import no.nav.syfo.service.MotebehovService;
import no.nav.syfo.service.VeilederTilgangService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.Pattern;
import javax.ws.rs.ForbiddenException;
import java.io.IOException;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.syfo.OIDCIssuer.AZURE;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@Slf4j
@RestController
@RequestMapping(value = "/api/enhet")
public class EnhetController {

    private MotebehovService motebehovService;

    private VeilederTilgangService veilederTilgangService;

    public EnhetController(final MotebehovService motebehovService,
                           final VeilederTilgangService veilederTilgangService) {
        this.motebehovService = motebehovService;
        this.veilederTilgangService = veilederTilgangService;
    }

    @ProtectedWithClaims(issuer = AZURE)
    @GetMapping(value = "/{enhet}/motebehov/brukere", produces = APPLICATION_JSON)
    public List<BrukerPaaEnhet> hentSykmeldteMedMotebehovSvarPaaEnhet
            (@PathVariable @Pattern(regexp = "\\d{4}$") String enhet) {
        if (!veilederTilgangService.sjekkVeiledersTilgangTilEnhet(enhet))
            throw new ForbiddenException("Innlogget bruker har ikke tilgang til følgende enhet: " + enhet);
        return motebehovService.hentSykmeldteMedMotebehovPaaEnhet(enhet);
    }

    @ExceptionHandler({ForbiddenException.class})
    void handleForbiddenRequests(HttpServletResponse response) throws IOException {
        response.sendError(FORBIDDEN.value(), "Handling er forbudt");
    }
}
