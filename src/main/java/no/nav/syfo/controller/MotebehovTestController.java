package no.nav.syfo.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.spring.oidc.validation.api.Unprotected;
import no.nav.syfo.domain.rest.Fnr;
import no.nav.syfo.domain.rest.Motebehov;
import no.nav.syfo.domain.rest.VeilederOppgaveFeedItem;
import no.nav.syfo.service.MotebehovService;
import no.nav.syfo.util.Toggle;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.validation.constraints.Pattern;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@RequestMapping(value = "/api/motebehovtest")
public class MotebehovTestController {

    private MotebehovService motebehovService;

    @Inject
    public MotebehovTestController(final MotebehovService motebehovService) {
        this.motebehovService = motebehovService;
    }

    @Unprotected
    @ResponseBody
    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public List<Motebehov> hentMotebehovTestListe(@RequestParam(name = "fnr") @Pattern(regexp = "^[0-9]{11}$") String arbeidstakerFnr) {
        return motebehovService.hentMotebehovListe(Fnr.of(arbeidstakerFnr));
    }
}
