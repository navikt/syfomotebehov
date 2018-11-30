package no.nav.syfo.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.spring.oidc.validation.api.ProtectedWithClaims;
import no.nav.syfo.domain.rest.BrukerPaaEnhet;
import no.nav.syfo.service.MotebehovService;
import no.nav.syfo.service.VeilederTilgangService;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Pattern;
import javax.ws.rs.ForbiddenException;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.syfo.OIDCIssuer.INTERN;

@Slf4j
@RestController
@RequestMapping(value = "/api/enheter")
public class EnhetRessurs {

    private MotebehovService motebehovService;

    private VeilederTilgangService veilederTilgangService;

    public EnhetRessurs(final MotebehovService motebehovService,
                        final VeilederTilgangService veilederTilgangService) {
        this.motebehovService = motebehovService;
        this.veilederTilgangService = veilederTilgangService;
    }

    @ResponseBody
    @ProtectedWithClaims(issuer = INTERN)
    @GetMapping(value = "/{enhet}/sykmeldte", produces = APPLICATION_JSON)
    public List<BrukerPaaEnhet> hentSykmeldteMedMotebehovSvarPaaEnhet
            (@PathVariable @Pattern(regexp = "\\d{4}$") String enhet) {
        if (!veilederTilgangService.sjekkVeiledersTilgangTilEnhet(enhet))
            throw new ForbiddenException("Innlogget bruker har ikke tilgang til følgende enhet: " + enhet);
        return motebehovService.hentSykmeldteMedMotebehovPaaEnhet(enhet);
    }
}
