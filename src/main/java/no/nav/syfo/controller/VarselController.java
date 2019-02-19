package no.nav.syfo.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.spring.oidc.validation.api.ProtectedWithClaims;
import no.nav.security.spring.oidc.validation.api.Unprotected;
import no.nav.syfo.domain.rest.TredjepartsKontaktinfo;
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
//    @ProtectedWithClaims(issuer = INTERN, claimMap = {"sub=srvsyfoservice"})
    @Unprotected
    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public Response hentGeografiskTilgang() {

        TredjepartsKontaktinfo tredjepartsKontaktinfo = new TredjepartsKontaktinfo()
                .aktoerId("1303656999808")
                .epost("erik.gunnar.jansen@nav.no")
                .mobil("95134909")
                .orgnummer("995816598");

        varselService.sendVarselTilNaermesteLeder(tredjepartsKontaktinfo);

        return Response
                .ok()
                .build();
    }
}
