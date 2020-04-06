package no.nav.syfo.controller.azuread;

import no.nav.security.oidc.api.ProtectedWithClaims;
import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.syfo.aktorregister.domain.Fodselsnummer;
import no.nav.syfo.domain.rest.Motebehov;
import no.nav.syfo.historikk.*;
import no.nav.syfo.service.*;
import no.nav.syfo.util.Metrikk;
import no.nav.syfo.veiledertilgang.VeilederTilgangConsumer;
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

    private final VeilederTilgangConsumer veilederTilgangConsumer;

    @Inject
    public MotebehovVeilederADController(
            OIDCRequestContextHolder oidcCtxHolder,
            Metrikk metrikk,
            HistorikkService historikkService,
            MotebehovService motebehovService,
            VeilederTilgangConsumer tilgangService
    ) {
        this.oidcCtxHolder = oidcCtxHolder;
        this.metrikk = metrikk;
        this.historikkService = historikkService;
        this.motebehovService = motebehovService;
        this.veilederTilgangConsumer = tilgangService;
    }

    @GetMapping(value = "/motebehov", produces = APPLICATION_JSON_VALUE)
    public List<Motebehov> hentMotebehovListe(
            @RequestParam(name = "fnr") @Pattern(regexp = "^[0-9]{11}$") String sykmeldtFnr
    ) {
        metrikk.tellEndepunktKall("veileder_hent_motebehov");

        Fodselsnummer fnr = new Fodselsnummer(sykmeldtFnr);

        kastExceptionHvisIkkeTilgang(fnr);

        return motebehovService.hentMotebehovListe(new Fodselsnummer(fnr.getValue()));
    }

    @GetMapping(value = "/historikk", produces = APPLICATION_JSON_VALUE)
    public List<Historikk> hentMotebehovHistorikk(
            @RequestParam(name = "fnr") @Pattern(regexp = "^[0-9]{11}$") String sykmeldtFnr
    ) {
        metrikk.tellEndepunktKall("veileder_hent_motebehov_historikk");

        Fodselsnummer fnr = new Fodselsnummer(sykmeldtFnr);

        kastExceptionHvisIkkeTilgang(fnr);

        return historikkService.hentHistorikkListe(fnr.getValue());
    }

    @PostMapping(value = "/motebehov/{fnr}/behandle")
    public void behandleMotebehov(
            @PathVariable(name = "fnr") @Pattern(regexp = "^[0-9]{11}$") String sykmeldtFnr
    ) {
        metrikk.tellEndepunktKall("veileder_behandle_motebehov_call");

        Fodselsnummer fnr = new Fodselsnummer(sykmeldtFnr);

        kastExceptionHvisIkkeTilgang(fnr);

        motebehovService.behandleUbehandledeMotebehov(fnr, getSubjectInternAD(oidcCtxHolder));

        metrikk.tellEndepunktKall("veileder_behandle_motebehov_success");
    }

    private void kastExceptionHvisIkkeTilgang(Fodselsnummer fnr) {
        if (!veilederTilgangConsumer.sjekkVeiledersTilgangTilPerson(fnr)) {
            throw new ForbiddenException("Veilederen har ikke tilgang til denne personen");
        }
    }
}
