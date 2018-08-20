package no.nav.syfo.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.spring.oidc.validation.api.ProtectedWithClaims;
import no.nav.syfo.domain.rest.VeilederOppgaveFeedItem;
import no.nav.syfo.service.MotebehovService;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@ProtectedWithClaims(issuer = "selvbetjening", claimMap = {"acr=Level4"})
@RequestMapping(value = "/api/feed/motebehov")
public class MotebehovOppgaveFeedController {

    private MotebehovService motebehovService;

    @Inject
    public MotebehovOppgaveFeedController(MotebehovService motebehovService) {
        this.motebehovService = motebehovService;
    }

    @ResponseBody
    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public List<VeilederOppgaveFeedItem> hentMotebehovListe(@RequestParam("timestamp") String timestamp) {
        return motebehovService.hentMotebehovListe(timestamp);
    }
}
