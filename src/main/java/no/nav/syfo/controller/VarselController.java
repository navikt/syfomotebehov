package no.nav.syfo.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.spring.oidc.validation.api.ProtectedWithClaims;
import no.nav.syfo.domain.rest.MotebehovsvarVarselInfo;
import no.nav.syfo.service.VarselService;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import static no.nav.syfo.OIDCIssuer.INTERN;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@RequestMapping(value = "/api/varsel/naermesteleder")
public class VarselController {

    private VarselService varselService;

    @Inject
    public VarselController(
            final VarselService varselService
    ) {
        this.varselService = varselService;
    }

    @ResponseBody
    @ProtectedWithClaims(issuer = INTERN, claimMap = {"sub=srvsyfoservice"})
    @PostMapping(produces = APPLICATION_JSON_VALUE)
    public Response sendVarselNaermesteLeder(
            @RequestBody MotebehovsvarVarselInfo motebehovsvarVarselInfo
    ) {

        varselService.sendVarselTilNaermesteLeder(motebehovsvarVarselInfo);

        return Response
                .ok()
                .build();
    }
}