package no.nav.syfo.controller.azuread;

import no.nav.security.oidc.api.ProtectedWithClaims;
import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.syfo.aktorregister.domain.Fodselsnummer;
import no.nav.syfo.domain.rest.Fnr;
import no.nav.syfo.domain.rest.Motebehov;
import no.nav.syfo.historikk.Historikk;
import no.nav.syfo.historikk.HistorikkService;
import no.nav.syfo.service.MotebehovService;
import no.nav.syfo.service.VeilederTilgangService;
import no.nav.syfo.util.Metrikk;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.validation.constraints.Pattern;
import javax.ws.rs.ForbiddenException;
import java.util.List;

import static no.nav.syfo.oidc.OIDCIssuer.AZURE;
import static no.nav.syfo.util.OIDCUtil.getSubjectInternAD;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@ProtectedWithClaims(issuer = AZURE)
@RequestMapping(value = "/api/internad/veileder")
public class MotebehovVeilederADController {

    private final OIDCRequestContextHolder oidcCtxHolder;

    private final Metrikk metrikk;

    private final HistorikkService historikkService;

    private final MotebehovService motebehovService;

    private final VeilederTilgangService veilederTilgangService;

    @Inject
    public MotebehovVeilederADController(
            OIDCRequestContextHolder oidcCtxHolder,
            Metrikk metrikk,
            HistorikkService historikkService,
            MotebehovService motebehovService,
            VeilederTilgangService tilgangService
    ) {
        this.oidcCtxHolder = oidcCtxHolder;
        this.metrikk = metrikk;
        this.historikkService = historikkService;
        this.motebehovService = motebehovService;
        this.veilederTilgangService = tilgangService;
    }

    @GetMapping(value = "/motebehov", produces = APPLICATION_JSON_VALUE)
    public List<Motebehov> hentMotebehovListe(
            @RequestParam(name = "fnr") @Pattern(regexp = "^[0-9]{11}$") String sykmeldtFnr
    ) {
        metrikk.tellEndepunktKall("veileder_hent_motebehov");

        Fnr fnr = Fnr.of(sykmeldtFnr);

        kastExceptionHvisIkkeTilgang(fnr);

        return motebehovService.hentMotebehovListe(new Fodselsnummer(fnr.getFnr()));
    }

    @GetMapping(value = "/historikk", produces = APPLICATION_JSON_VALUE)
    public List<Historikk> hentMotebehovHistorikk(
            @RequestParam(name = "fnr") @Pattern(regexp = "^[0-9]{11}$") String sykmeldtFnr
    ) {
        metrikk.tellEndepunktKall("veileder_hent_motebehov_historikk");

        Fnr fnr = Fnr.of(sykmeldtFnr);

        kastExceptionHvisIkkeTilgang(fnr);

        return historikkService.hentHistorikkListe(fnr.getFnr());
    }

    @PostMapping(value = "/motebehov/{fnr}/behandle")
    public void behandleMotebehov(
            @PathVariable(name = "fnr") @Pattern(regexp = "^[0-9]{11}$") String sykmeldtFnr
    ) {
        metrikk.tellEndepunktKall("veileder_behandle_motebehov_call");

        Fnr fnr = Fnr.of(sykmeldtFnr);

        kastExceptionHvisIkkeTilgang(fnr);

        motebehovService.behandleUbehandledeMotebehov(fnr, getSubjectInternAD(oidcCtxHolder));

        metrikk.tellEndepunktKall("veileder_behandle_motebehov_success");
    }

    private void kastExceptionHvisIkkeTilgang(Fnr fnr) {
        if (!veilederTilgangService.sjekkVeiledersTilgangTilPersonViaAzure(fnr)) {
            throw new ForbiddenException("Veilederen har ikke tilgang til denne personen");
        }
    }
}
