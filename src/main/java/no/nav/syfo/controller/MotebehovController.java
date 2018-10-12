package no.nav.syfo.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.oidc.OIDCConstants;
import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.security.oidc.context.OIDCValidationContext;
import no.nav.security.spring.oidc.validation.api.ProtectedWithClaims;
import no.nav.syfo.domain.rest.Fnr;
import no.nav.syfo.domain.rest.Motebehov;
import no.nav.syfo.domain.rest.NyttMotebehov;
import no.nav.syfo.service.MotebehovService;
import no.nav.syfo.service.TilgangBrukerService;
import no.nav.syfo.util.Toggle;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.ws.rs.ForbiddenException;
import java.io.IOException;
import java.util.List;

import static java.util.Collections.emptyList;
import static no.nav.syfo.OIDCIssuer.EKSTERN;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@ProtectedWithClaims(issuer = EKSTERN, claimMap = {"acr=Level4"})
@RequestMapping(value = "/api/motebehov")
public class MotebehovController {

    private OIDCRequestContextHolder contextHolder;
    private MotebehovService motebehovService;
    private TilgangBrukerService tilgangBrukerService;

    @Inject
    public MotebehovController(final OIDCRequestContextHolder contextHolder,
                               final MotebehovService motebehovService,
                               final TilgangBrukerService tilgangBrukerService
    ) {
        this.contextHolder = contextHolder;
        this.motebehovService = motebehovService;
        this.tilgangBrukerService = tilgangBrukerService;
    }

    @ResponseBody
    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public List<Motebehov> hentMotebehovListe(
            @RequestParam(name = "fnr") @Pattern(regexp = "^[0-9]{11}$") String arbeidstakerFnr,
            @RequestParam(name = "virksomhetsnummer") String virksomhetsnummer
    ) {
        if (Toggle.endepunkterForMotebehov) {
            Fnr fnr = Fnr.of(arbeidstakerFnr);
            if (!tilgangBrukerService.harTilgangTilOppslaattBruker(fnr.getFnr())) {
                throw new ForbiddenException();
            }
            if (!virksomhetsnummer.isEmpty()) {
                return motebehovService.hentMotebehovListe(fnr, virksomhetsnummer);
            }
            return motebehovService.hentMotebehovListe(fnr);
        } else {
            log.info("Det ble gjort kall mot 'motebehov', men dette endepunktet er togglet av.");
            return emptyList();
        }
    }

    @ResponseBody
    @PostMapping(consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public void lagreMotebehov(@RequestBody @Valid NyttMotebehov lagreMotebehov) {
        if (Toggle.endepunkterForMotebehov) {
            Fnr fnr = fnrFraOIDC();
            if (!tilgangBrukerService.harTilgangTilOppslaattBruker(fnr.getFnr())) {
                throw new ForbiddenException();
            }
            motebehovService.lagreMotebehov(fnr, lagreMotebehov);
        } else {
            log.info("Det ble gjort kall mot 'motebehov', men dette endepunktet er togglet av.");
        }
    }

    private Fnr fnrFraOIDC() {
        OIDCValidationContext context = (OIDCValidationContext) contextHolder
                .getRequestAttribute(OIDCConstants.OIDC_VALIDATION_CONTEXT);
        return Fnr.of(context.getClaims("selvbetjening").getClaimSet().getSubject());
    }


    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
    void handleBadRequests(HttpServletResponse response) throws IOException {
        response.sendError(BAD_REQUEST.value(), "Vi kunne ikke tolke inndataene :/");
    }

}
