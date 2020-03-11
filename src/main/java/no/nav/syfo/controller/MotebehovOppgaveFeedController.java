package no.nav.syfo.controller;

import no.nav.security.oidc.api.ProtectedWithClaims;
import no.nav.security.oidc.api.Unprotected;
import no.nav.syfo.domain.rest.VeilederOppgaveFeedItem;
import no.nav.syfo.service.MotebehovService;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.List;

import static no.nav.syfo.oidc.OIDCIssuer.INTERN;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@ProtectedWithClaims(issuer = INTERN, claimMap = {"sub=srvsyfoveilederoppgaver"})
@RequestMapping(value = "/api/feed/motebehov")
public class MotebehovOppgaveFeedController {

    private MotebehovService motebehovService;

    @Inject
    public MotebehovOppgaveFeedController(MotebehovService motebehovService) {
        this.motebehovService = motebehovService;
    }

    @Unprotected
    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public List<VeilederOppgaveFeedItem> hentMotebehovListe(
            @RequestParam("timestamp") String timestamp
    ) {
        return motebehovService.hentMotebehovListe(timestamp);
    }
}
