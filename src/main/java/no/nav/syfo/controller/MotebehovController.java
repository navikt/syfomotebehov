package no.nav.syfo.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.oidc.OIDCConstants;
import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.security.oidc.context.OIDCValidationContext;
import no.nav.security.spring.oidc.validation.api.ProtectedWithClaims;
import no.nav.syfo.domain.rest.Fnr;
import no.nav.syfo.domain.rest.LagreMotebehov;
import no.nav.syfo.domain.rest.Motebehov;
import no.nav.syfo.service.MotebehovService;
import no.nav.syfo.util.Toggle;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@ProtectedWithClaims(issuer = "selvbetjening", claimMap = {"acr=Level4"})
@RequestMapping(value = "/api/motebehov")
public class MotebehovController {

    private OIDCRequestContextHolder contextHolder;
    private MotebehovService motebehovService;

    @Inject
    public MotebehovController(final OIDCRequestContextHolder contextHolder,
                               final MotebehovService motebehovService){
        this.contextHolder = contextHolder;
        this.motebehovService = motebehovService;
    }

    @ResponseBody
    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public List<Motebehov> hentMotebehovListe(@RequestParam(name = "fnr") @Pattern(regexp = "^[0-9]{11}$") String arbeidstakerFnr) {
        if (Toggle.endepunkterForMotebehov) {
            return motebehovService.hentMotebehovListe(Fnr.of(arbeidstakerFnr));
        } else {
            log.info("Det ble gjort kall mot 'motebehov', men dette endepunktet er togglet av.");
            return emptyList();
        }
    }

    @ResponseBody
    @PostMapping(consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public UUID lagreMotebehov(@RequestBody @Valid LagreMotebehov lagreMotebehov) {
        if (Toggle.endepunkterForMotebehov) {
            return motebehovService.lagreMotebehov(fnrFraOIDC(), lagreMotebehov);
        } else {
            log.info("Det ble gjort kall mot 'motebehov', men dette endepunktet er togglet av.");
            return null;
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
